package br.com.engine.input;

public enum KeyMap 
{
    UP     (KeyCode.UP.getCode()),
    DOWN   (KeyCode.DOWN.getCode()),
    LEFT   (KeyCode.LEFT.getCode()),
    RIGHT  (KeyCode.RIGHT.getCode()),
    SPACE  (KeyCode.SPACE.getCode()),
    ESCAPE (KeyCode.ESCAPE.getCode()),
    ENTER  (KeyCode.ENTER.getCode()),
    CONTROL(KeyCode.CONTROL.getCode()),
    SHIFT  (KeyCode.SHIFT.getCode()),
    ALT    (KeyCode.ALT.getCode()),
    TAB    (KeyCode.TAB.getCode()),
    W      (KeyCode.W.getCode()),
    A      (KeyCode.A.getCode()),
    S      (KeyCode.S.getCode()),
    D      (KeyCode.D.getCode()),
    Q      (KeyCode.Q.getCode()),
    E      (KeyCode.E.getCode()),
    Z      (KeyCode.Z.getCode()),
    X      (KeyCode.X.getCode()),
    C      (KeyCode.C.getCode());

    private int key;

    KeyMap( int key )
    {
        this.key = key;
    }

    public int getKey( )
    {
        return key;
    }
}
