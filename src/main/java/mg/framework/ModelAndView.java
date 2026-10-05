package mg.framework;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String url;
    private Map<String, Object> data;

    public ModelAndView() {
        this.data = new HashMap<>();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAttribute(String nom, Object valeur) {
        this.data.put(nom, valeur);
    }

    public String getUrl() {
        return this.url;
    }

    public Map<String, Object> getData() {
        return this.data;
    }
}
