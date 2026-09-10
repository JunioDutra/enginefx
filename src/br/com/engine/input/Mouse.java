package br.com.engine.input;

import java.util.ArrayList;
import java.util.List;
import br.com.engine.interfaces.IMouseClick;

/** Ordered subscriptions. Removing a listener during dispatch prevents subsequent invocation. */
public final class Mouse
{
    private static final Mouse INSTANCE = new Mouse();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private Mouse() { }
    public static Mouse infInstace() { return INSTANCE; }

    private final class Subscription implements InputSubscription
    {
        final Object owner;
        final IMouseClick listener;
        boolean closed;
        Subscription(Object owner, IMouseClick listener) { this.owner = owner; this.listener = listener; }
        @Override public void close() { closed = true; subscriptions.remove(this); }
    }

    public InputSubscription addListener(Object owner, IMouseClick listener)
    {
        if (owner == null || listener == null) throw new IllegalArgumentException("Mouse listener and owner are required");
        for (Subscription subscription : subscriptions)
            if (subscription.owner == owner && subscription.listener == listener) return subscription;
        Subscription subscription = new Subscription(owner, listener);
        subscriptions.add(subscription);
        return subscription;
    }

    public void click(double x, double y)
    {
        MouseEvent event = new MouseEvent(x, y);
        for (Subscription subscription : List.copyOf(subscriptions))
            if (!subscription.closed) subscription.listener.onClick(event);
    }

    public void releaseOwner(Object owner)
    {
        for (Subscription subscription : List.copyOf(subscriptions))
            if (subscription.owner == owner) subscription.close();
    }
}
