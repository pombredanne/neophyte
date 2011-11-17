/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.neo4j.helpers.Format;

public abstract class StringLogger
{
    public static final String DEFAULT_NAME = "messages.log";
    public static final StringLogger SYSTEM =
        new ActualStringLogger( new PrintWriter( System.out ) );
    private static final int DEFAULT_THRESHOLD_FOR_ROTATION = 100 * 1024 * 1024;
    private static final int NUMBER_OF_OLD_LOGS_TO_KEEP = 2;
    
    public static StringLogger logger( String storeDir )
    {
        return logger( storeDir, DEFAULT_THRESHOLD_FOR_ROTATION );
    }

    public static StringLogger logger( String storeDir, int rotationThreshold )
    {
        return new ActualStringLogger( new File( storeDir, DEFAULT_NAME ).getAbsolutePath(),
                rotationThreshold );
    }
    
    public abstract void logMessage( String msg );

    public abstract void logMessage( String msg, Throwable cause );

    public abstract void logMessage( String msg, boolean flush );

    public abstract void logMessage( String msg, Throwable cause, boolean flush );

    public abstract void flush();

    public abstract void close();
    
    public static final StringLogger DEV_NULL = new StringLogger()
    {
        @Override
        public void logMessage( String msg ) {}

        @Override
        public void logMessage( String msg, Throwable cause ) {}

        @Override
        public synchronized void logMessage( String msg, boolean flush ) {}

        @Override
        public synchronized void logMessage( String msg, Throwable cause, boolean flush ) {}

        @Override
        public void flush() {}

        @Override
        public void close() {}
    };
    
    private static class ActualStringLogger extends StringLogger
    {
        private PrintWriter out;
        private final Integer rotationThreshold;
        private final File file;
        
        private ActualStringLogger( String filename, int rotationThreshold )
        {
            this.rotationThreshold = rotationThreshold;
            try
            {
                file = new File( filename );
                if ( file.getParentFile() != null )
                {
                    file.getParentFile().mkdirs();
                }
                instantiateWriter();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        
        private ActualStringLogger( PrintWriter writer )
        {
            this.out = writer;
            this.rotationThreshold = null;
            this.file = null;
        }
    
        private void instantiateWriter() throws IOException
        {
            out = new PrintWriter( new FileWriter( file, true ) );
        }
    
        public void logMessage( String msg )
        {
            logMessage( msg, false );
        }
    
        public void logMessage( String msg, Throwable cause )
        {
            logMessage( msg, cause, false );
        }
    
        public synchronized void logMessage( String msg, boolean flush )
        {
//            ensureOpen();
            out.println( time() + ": " + msg );
            if ( flush )
            {
                out.flush();
            }
            checkRotation();
        }
    
        private String time()
        {
            return Format.date();
        }
    
        public synchronized void logMessage( String msg, Throwable cause, boolean flush )
        {
//            ensureOpen();
            out.println( time() + ": " + msg + " " + cause.getMessage() );
            cause.printStackTrace( out );
            if ( flush )
            {
                out.flush();
            }
            checkRotation();
        }
    
//        private void ensureOpen()
//        {
//            /*
//             * Since StringLogger has instances in its own static map and HA graph db
//             * does internal restarts of the database the StringLogger instances are kept
//             * whereas the actual files can be removed/replaced, making the PrintWriter
//             * fail at writing stuff and also swallowing those exceptions(!). Since we
//             * have this layout of static map of loggers we'll have to reopen the PrintWriter
//             * in such occasions. It'd be better to tie each StringLogger to a GraphDatabaseService.
//             */
//            if ( out.checkError() )
//            {
//                out.close();
//                try
//                {
//                    instantiateWriter();
//                }
//                catch ( IOException e )
//                {
//                    throw new RuntimeException( e );
//                }
//            }
//        }
    
        private void checkRotation()
        {
            if ( rotationThreshold != null && file.length() > rotationThreshold.intValue() )
            {
                doRotation();
            }
        }
    
        private void doRotation()
        {
            out.close();
            moveAwayFile();
            try
            {
                instantiateWriter();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    
        /**
         * Will move:
         * messages.log.1 -> messages.log.2
         * messages.log   -> messages.log.1
         *
         * Will delete (if exists):
         * messages.log.2
         */
        private void moveAwayFile()
        {
            File oldLogFile = new File( file.getParentFile(), file.getName() + "." + NUMBER_OF_OLD_LOGS_TO_KEEP );
            if ( oldLogFile.exists() )
            {
                oldLogFile.delete();
            }
    
            for ( int i = NUMBER_OF_OLD_LOGS_TO_KEEP-1; i >= 0; i-- )
            {
                oldLogFile = new File( file.getParentFile(), file.getName() + (i == 0 ? "" : ("." + i)) );
                if ( oldLogFile.exists() )
                {
                    oldLogFile.renameTo( new File( file.getParentFile(), file.getName() + "." + (i+1) ) );
                }
            }
        }
    
        public void flush()
        {
            out.flush();
        }
    
        public void close()
        {
            out.close();
        }
    }
}
