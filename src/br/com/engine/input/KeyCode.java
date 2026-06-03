package br.com.engine.input;

public enum KeyCode
{
    UP( org.lwjgl.glfw.GLFW.GLFW_KEY_UP ),
    DOWN( org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN ),
    LEFT( org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT ),
    RIGHT( org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT ),
    SPACE( org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE ),
    ENTER( org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER ),
    ESCAPE( org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE ),
    CONTROL( org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL ),
    SHIFT( org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT ),
    ALT( org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT ),
    TAB( org.lwjgl.glfw.GLFW.GLFW_KEY_TAB ),
    W( org.lwjgl.glfw.GLFW.GLFW_KEY_W ),
    A( org.lwjgl.glfw.GLFW.GLFW_KEY_A ),
    S( org.lwjgl.glfw.GLFW.GLFW_KEY_S ),
    D( org.lwjgl.glfw.GLFW.GLFW_KEY_D ),
    Q( org.lwjgl.glfw.GLFW.GLFW_KEY_Q ),
    E( org.lwjgl.glfw.GLFW.GLFW_KEY_E ),
    Z( org.lwjgl.glfw.GLFW.GLFW_KEY_Z ),
    X( org.lwjgl.glfw.GLFW.GLFW_KEY_X ),
    C( org.lwjgl.glfw.GLFW.GLFW_KEY_C );

    private final int code;

    KeyCode( int code )
    {
        this.code = code;
    }

    public int getCode( )
    {
        return code;
    }

    public static KeyCode fromGlfw( int glfwKeyCode )
    {
        for( KeyCode k : values( ) )
        {
            if( k.code == glfwKeyCode )
            {
                return k;
            }
        }
        return null;
    }
}
