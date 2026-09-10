package br.com.engine.componentes.scripts;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.input.KeyBoard;
import br.com.engine.input.KeyCode;

/** Upward jump phase, in pixels and pixels/second. Falling is supplied by the game's physics. */
public class JumpSC extends SimpleComponent
{
    private final int maxJumpSize;
    private final int jumpVelelocity;
    private float inicialPosY;
    private boolean jumping;

    public JumpSC() { this(50, 300); }

    public JumpSC(int maxJumpSize, int jumpVelelocity)
    {
        if (maxJumpSize <= 0 || jumpVelelocity <= 0) throw new IllegalArgumentException("Jump height and speed must be positive");
        this.maxJumpSize = maxJumpSize;
        this.jumpVelelocity = jumpVelelocity;
    }

    @Override public void setup() { inicialPosY = getParent().getPosition().y; }
    @Override public void update(long time) { }
    @Override public void draw() { }

    @Override public void fixedUpdate(float deltaSeconds)
    {
        KeyBoard.infInstace().ifKeyPressed(KeyCode.SPACE, () -> {
            if (Math.abs(inicialPosY - getParent().getPosition().y) < 0.001f) jumping = true;
        });
        if (!jumping) return;
        float target = inicialPosY - maxJumpSize;
        getParent().getPosition().y = Math.max(target, getParent().getPosition().y - jumpVelelocity * deltaSeconds);
        if (getParent().getPosition().y <= target) jumping = false;
    }
}
