package br.com.engine.resources;

import java.io.IOException;
import java.net.JarURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Materializes packaged assets for legacy loaders that require real files (TMX, TTF, audio). */
final class PackagedResources
{
    static String root( )
    {
        var resource = ContentLoader.class.getResource( "/res/application.json" );
        if( resource == null ) return null;
        try
        {
            if( resource.getProtocol( ).equals( "file" ) ) return Path.of( resource.toURI( ) ).getParent( ).toString( );
            if( !resource.getProtocol( ).equals( "jar" ) ) return null;
            JarURLConnection connection = (JarURLConnection)resource.openConnection( );
            connection.setUseCaches( false );
            Path root = Files.createTempDirectory( "enginefx-assets-" );
            try( var jar = connection.getJarFile( ) )
            {
                var entries = jar.entries( );
                while( entries.hasMoreElements( ) )
                {
                    var entry = entries.nextElement( );
                    if( entry.isDirectory( ) || !entry.getName( ).startsWith( "res/" ) ) continue;
                    Path target = root.resolve( entry.getName( ).substring( 4 ) ).normalize( );
                    if( !target.startsWith( root ) ) throw new IOException( "Invalid asset path: " + entry.getName( ) );
                    Files.createDirectories( target.getParent( ) );
                    try( var input = jar.getInputStream( entry ) ) { Files.copy( input, target ); }
                }
            }
            finally
            {
                // deleteOnExit runs in reverse registration order: files before parents.
                try( var paths = Files.walk( root ) )
                {
                    paths.sorted( Comparator.comparingInt( Path::getNameCount ) ).forEach( p -> p.toFile( ).deleteOnExit( ) );
                }
            }
            return root.toString( );
        }
        catch( Exception exception ) { throw new IllegalStateException( "Cannot load packaged game assets", exception ); }
    }
}
