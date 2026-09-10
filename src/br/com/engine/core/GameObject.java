package br.com.engine.core;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.builder.ToStringBuilder;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.TypeComponents;
import br.com.engine.componentes.VectorMonitor;
import br.com.engine.input.Mouse;
import br.com.engine.interfaces.IComponent;

public class GameObject
{
    private String tag = "";
    private final Vector2 previusPosition = new Vector2();
    private final Vector2 position = new Vector2();
    private final List<IComponent> componentes = new ArrayList<>();
    private final List<IComponent> pendingComponents = new ArrayList<>();
    private final List<GameObject> objetosFilhos = new ArrayList<>();
    private GameObject pai;
    private boolean bDestroy;
    private boolean initialized;
    private boolean initializing;
    private boolean disposed;
    private boolean flushing;
    private int callbackDepth;

    public GameObject() { }
    public GameObject(String tag) { this.tag = tag; }

    public void setup()
    {
        if (disposed) throw new IllegalStateException("Cannot set up a disposed object");
        if (initialized || initializing) return;
        initializing = true;
        addComponente(new VectorMonitor((vec, oldVec) -> {
            Vector2 difference = vec.diff(oldVec);
            for (GameObject child : List.copyOf(objetosFilhos))
                if (!child.disposed) child.getPosition().plus(difference);
        }));
        try
        {
            int processed = 0;
            while (!disposed && processed < componentes.size())
            {
                IComponent component = componentes.get(processed++);
                callbackDepth++;
                try { component.setup(); }
                finally { callbackDepth--; flushPendingComponents(); }
            }
            initialized = !disposed;
            capturePosition();
        }
        catch (RuntimeException exception)
        {
            try { dispose(); } catch (RuntimeException cleanup) { exception.addSuppressed(cleanup); }
            throw exception;
        }
        finally { initializing = false; }
    }

    public Vector2 getPosition() { return position; }
    /** Last accepted physics position, used to roll back a collision. */
    public Vector2 getPreviusPosition() { return previusPosition; }
    void capturePosition() { previusPosition.setPosition(position.x, position.y); }
    public List<IComponent> getComponentes() { return Collections.unmodifiableList(componentes); }

    public void addComponente(IComponent component)
    {
        if (component == null) throw new IllegalArgumentException("Component cannot be null");
        if (disposed) throw new IllegalStateException("Cannot add a component to a disposed object");
        if (componentes.contains(component) || pendingComponents.contains(component)) return;
        if (component instanceof SimpleComponent simple)
        {
            if (simple.getParent() != null && simple.getParent() != this)
                throw new IllegalArgumentException("Component already belongs to another object");
            simple.setParent(this);
        }
        pendingComponents.add(component);
        flushPendingComponents();
    }

    public Object getComponent(TypeComponents type) { return getComponent(type.getValue()); }
    public Object getComponent(String name) { return getComponent(TypeComponents.valueOf(name.toUpperCase())); }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> type)
    {
        if (type.isArray())
        {
            List<IComponent> matches = componentes.stream().filter(type.getComponentType()::isInstance).toList();
            Object result = Array.newInstance(type.getComponentType(), matches.size());
            for (int i = 0; i < matches.size(); i++) Array.set(result, i, matches.get(i));
            return (T)result;
        }
        T result = null;
        for (IComponent component : componentes) if (type.isInstance(component)) result = type.cast(component);
        return result;
    }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public void destroy() { bDestroy = true; }
    public boolean isDestroy() { return bDestroy; }
    public boolean isDisposed() { return disposed; }

    void updateComponents(long time)
    {
        runComponents(component -> { if (!(component instanceof VectorMonitor)) component.update(time); });
    }

    void fixedUpdateComponents(float deltaSeconds)
    {
        runComponents(component -> component.fixedUpdate(deltaSeconds));
    }

    void synchronizePosition()
    {
        if (pai != null) pai.synchronizePosition();
        runComponents(component -> { if (component instanceof VectorMonitor) component.update(0); });
    }

    void drawComponents() { runComponents(IComponent::draw); }

    private void runComponents(Consumer<IComponent> action)
    {
        if (disposed) return;
        callbackDepth++;
        try
        {
            for (IComponent component : List.copyOf(componentes))
            {
                if (disposed || bDestroy) break;
                action.accept(component);
            }
        }
        finally { callbackDepth--; flushPendingComponents(); }
    }

    private void flushPendingComponents()
    {
        if (callbackDepth != 0 || flushing || disposed) return;
        flushing = true;
        try
        {
            while (!pendingComponents.isEmpty() && !disposed)
            {
                IComponent component = pendingComponents.remove(0);
                componentes.add(component);
                if (initialized)
                {
                    callbackDepth++;
                    try { component.setup(); }
                    catch (RuntimeException exception)
                    {
                        componentes.remove(component);
                        Mouse.infInstace().releaseOwner(component);
                        try { component.dispose(); } catch (RuntimeException cleanup) { exception.addSuppressed(cleanup); }
                        throw exception;
                    }
                    finally { callbackDepth--; }
                }
            }
        }
        finally { flushing = false; }
    }

    public void dispose()
    {
        if (disposed) return;
        disposed = true;
        bDestroy = true;
        Mouse.infInstace().releaseOwner(this);
        RuntimeException failure = null;
        List<IComponent> all = new ArrayList<>(componentes);
        all.addAll(pendingComponents);
        componentes.clear();
        pendingComponents.clear();
        for (IComponent component : all)
        {
            Mouse.infInstace().releaseOwner(component);
            try { component.dispose(); } catch (RuntimeException exception) { failure = combine(failure, exception); }
        }
        for (GameObject child : List.copyOf(objetosFilhos))
            try { child.dispose(); } catch (RuntimeException exception) { failure = combine(failure, exception); }
        objetosFilhos.clear();
        if (pai != null) pai.objetosFilhos.remove(this);
        pai = null;
        if (failure != null) throw failure;
    }

    static RuntimeException combine(RuntimeException first, RuntimeException next)
    {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    public GameObject getPai() { return pai; }

    public void setPai(GameObject parent)
    {
        if (disposed || (parent != null && parent.disposed)) throw new IllegalStateException("Disposed parent/child");
        for (GameObject ancestor = parent; ancestor != null; ancestor = ancestor.pai)
            if (ancestor == this) throw new IllegalArgumentException("Parent cycle");
        if (pai == parent) return;
        if (pai != null) pai.objetosFilhos.remove(this);
        pai = parent;
        if (parent != null) parent.objetosFilhos.add(this);
    }

    @Override public String toString() { return ToStringBuilder.reflectionToString(this); }
}
