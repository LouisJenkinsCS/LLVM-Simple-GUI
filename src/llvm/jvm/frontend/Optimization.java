/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package llvm.jvm.frontend;

import java.time.Instant;

/**
 *
 * @author Louis Jenkins
 */
public class Optimization {
    public boolean selected;
    public String name;
    public String arg;
    public String description;
    public Instant timeSelected;

    public Instant getTimeSelected() {
        return timeSelected;
    }

    public void setTimeSelected(Instant timeSelected) {
        this.timeSelected = timeSelected;
    }

    public Optimization(String name, String description) {
        this.selected = false;
        this.name = name;
        this.description = description;
    }
    

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        this.timeSelected = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
    
}
