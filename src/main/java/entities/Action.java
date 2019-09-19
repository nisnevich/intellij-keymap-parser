package entities;

import java.util.List;

public class Action {
    private String id;
    private String description;
    private String fullTextDescription;
    private List<Shortcut> shortcuts;
    private String actionGroup;

    public String getFullTextDescription() {
        return fullTextDescription;
    }

    public void setFullTextDescription(String fullTextDescription) {
        this.fullTextDescription = fullTextDescription;
    }

    public List<Shortcut> getShortcuts() {
        return shortcuts;
    }

    public void setShortcuts(List<Shortcut> shortcuts) {
        this.shortcuts = shortcuts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActionGroup() {
        return actionGroup;
    }

    public void setActionGroup(String actionGroup) {
        this.actionGroup = actionGroup;
    }
}
