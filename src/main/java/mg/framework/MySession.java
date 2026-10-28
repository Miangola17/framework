package mg.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MySession {
    
    private Map<String, Object> data;
    private Set<String> initialKeys;
    
    public MySession() {
        this.data = new HashMap<>();
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setInitialKeys(Set<String> keys) {
        this.initialKeys = keys;
    }
    
    public Set<String> getInitialKeys() {
        return this.initialKeys;
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public void add(String key, Object value) {
        data.put(key, value);
    }
    
    public void remove(String key) {
        data.remove(key);
    }
    
    public Set<String> getNames() {
        return data.keySet();
    }
}
