package se.sics.mspsim.util;
import java.util.ArrayList;

public class ComponentRegistry {

  private ArrayList<ComponentEntry> components = new ArrayList<ComponentEntry>();
  
  public void registerComponent(String name, Object component) {
    synchronized (components) {
      components.add(new ComponentEntry(name, component)); 
    }
    if (component instanceof ActiveComponent) {
      ((ActiveComponent)component).setComponentRegistry(this);
    }
  }
  
  public synchronized Object getComponent(String name) {
    for (int i = 0, n = components.size(); i < n; i++) {
      if (name.equals(components.get(i).name)) {
        return components.get(i).component;
      }
    }
    return null;
  }
  
  @SuppressWarnings("unchecked")
  public synchronized Object getComponent(Class name) {
    for (int i = 0, n = components.size(); i < n; i++) {
      if (name.isAssignableFrom(components.get(i).component.getClass())) {
        return components.get(i).component;
      }
    }
    return null;
  }
  
  @SuppressWarnings("unchecked")
  public synchronized Object[] getAllComponents(Class name) {
    return null;
  }

  public void start() {
    ComponentEntry[] plugs;
    synchronized (this) {
      plugs = components.toArray(new ComponentEntry[components.size()]);
    }
    for (int i = 0; i < plugs.length; i++) {
      if (plugs[i].component instanceof ActiveComponent) {
        ((ActiveComponent) plugs[i].component).start();
      }
    }
  }
  
  private static class ComponentEntry {
    public final String name;
    public final Object component;
    
    private ComponentEntry(String name, Object component) {
      this.name = name;
      this.component = component;
    }
  }
}