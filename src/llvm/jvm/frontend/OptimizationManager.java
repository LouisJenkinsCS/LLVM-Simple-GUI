/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package llvm.jvm.frontend;

import io.reactivex.Observable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Louis Jenkins
 */
public class OptimizationManager {

    // We maintain a global mapping of optimization arguments to their
    // respective descriptions that serve as tooltip information.
    private static final List<Optimization> optimizations = new ArrayList<>();

    public static void init() throws ParseException, IOException {
        ((Map<String, String>) (JSONObject) new JSONParser()
                .parse(new String(Files
                        .readAllBytes(Paths
                                .get("opt-passes.json")
                        )
                )))
                .forEach((k, v) -> {
                    Optimization opt = new Optimization(k,v);
                    if (k.equals("mem2reg")) opt.setSelected(true);
                    optimizations.add(opt);
                });
    }

    public static Observable<Optimization> getOptimizations() {
        return Observable
                .fromIterable(optimizations)
                .sorted((opt1, opt2) -> opt1
                        .getName()
                        .compareTo(opt2.getName())
                );
    }

    // Reset all selected optimizations
    public static void reset() {
        getOptimizations()
                .forEach(opt -> opt.setSelected(true));
    }
    
    public static Observable<Optimization> getSelected() {
        return getOptimizations()
                .filter(Optimization::isSelected)
                .sorted((opt1, opt2) -> opt1
                        .getTimeSelected()
                        .compareTo(opt2.getTimeSelected())
                );
    }
    
    public static void setSelected(String optName, boolean isSelected) {
        getOptimizations()
                .filter(opt -> opt.getName().equals(optName))
                .subscribe(opt -> opt.setSelected(isSelected));
    }
}
