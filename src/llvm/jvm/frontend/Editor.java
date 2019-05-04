/* BSD 3-Clause License
 *
 * Copyright (c) 2017, Louis Jenkins <LouisJenkinsCS@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of Louis Jenkins, Bloomsburg University nor the names of its 
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package llvm.jvm.frontend;

import com.ngs.image.ImageModel;
import com.ngs.image.ImagePanel;
import com.ngs.image.ImageSource;
import com.ngs.image.source.ImageIOSource;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Louis Jenkins
 */
public class Editor extends javax.swing.JFrame {
    
    private int graphType = CFG;
    private static final int CFG = 0, CFG_ONLY = 1, DOM = 2, DOM_ONLY = 3,  POSTDOM = 4, POSTDOM_ONLY = 5;

    private static String LLVM_JVM_PATH = "/home/awsgui/LLVM-JVM/";
    
    private File dir;

    private RSyntaxTextArea editor;
    private static String defaultContents = "int main(void) {\n\treturn 0;\n}";
    
    private DefaultListModel<JCheckBox> model;
    /**
     * Creates new form Editor
     */
    public Editor() {
        try {
            initComponents();
            model = new DefaultListModel<>();
            CheckBoxList cblist = new CheckBoxList(model);
            
            OptimizationManager.init();
            OptimizationManager.getOptimizations().forEach(opt -> {
                JCheckBox checkbox = new JCheckBox(opt.getName());
                checkbox.setToolTipText(opt.getDescription());
                model.addElement(checkbox);
            });
            cblist.addListSelectionListener(e -> {
                CheckBoxList lsm = (CheckBoxList)e.getSource();
                
                if (lsm.getValueIsAdjusting()) {
                    return;
                }
                if (lsm.isSelectionEmpty()) {
                    OptimizationManager.reset();
                    System.out.println("Reset...");
                } else {
                    // Find out which indexes are selected.
                    JCheckBox chbox = lsm.getSelectedValue();
                    OptimizationManager.setSelected(chbox.getText(), true);
                }
            });
            OptimizationsPanel.add(new JScrollPane(cblist), BorderLayout.CENTER);
                    
            editor = new RSyntaxTextArea(20, 40);
            editor.setFont(new Font("Arial", Font.PLAIN, 20));
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
            editor.setCodeFoldingEnabled(true);
            editor.setText(defaultContents);
            RTextScrollPane sp = new RTextScrollPane(editor);
            TextEditorPanel.add(sp);

            setTitle("Text Editor Demo");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            pack();
            setLocationRelativeTo(null);
        } catch (ParseException | IOException ex) {
            Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static String getBaseName(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index == -1) {
            return fileName;
        } else {
            return fileName.substring(0, index);
        }
    }

    private void generateLLVMGraph(int... optimizations) throws IOException, InterruptedException {
        ArrayList<String> args = new ArrayList<>();
        String graphTypeString = 
                graphType == CFG ? "cfg" 
                : graphType == CFG_ONLY ? "cfg-only" 
                : graphType == DOM ? "dom" 
                : graphType == DOM_ONLY ? "dom-only"
                : graphType == POSTDOM ? "postdom"
                : "postdom-only";
        args.add("opt");
        args.add(dir + "/unoptimized.bc");
        args.add("-instnamer");
        
        
        OptimizationManager.reset();
        // Remove any unselected JCheckBox's, as unselection is not triggered
        // by the ListSelectionListener event.
        for(int i = 0; i < model.size(); i++) {
            JCheckBox cbox = model.elementAt(i);
            if(!cbox.isSelected()) {
                OptimizationManager
                        .getSelected()
                        .filter(opt -> opt.getName().equals(cbox.getText()))
                        .subscribe(opt -> opt.setSelected(false));
            }
        }
        OptimizationManager
                .getSelected()
                .forEach(opt -> args.add("-" + opt.getName())); 
        args.add("-dot-" + graphTypeString);
                
        System.out.println(args);
        new ProcessBuilder()
                .command(args)
                .directory(dir)
                .start()
                .waitFor();
        
        File dotFile = null;
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".dot")) {
                dotFile = f;
                break;
            }
        }
        if (dotFile == null) {
            throw new IOException("Could not find a *.dot file!");
        }
        
        new ProcessBuilder()
                .command("dot", "-Tpng", dotFile.getAbsolutePath() , "-o", "unoptimizedIR.png")
                .directory(dir)
                .start()
                .waitFor();

        ImageSource src = new ImageIOSource(Paths.get(dir + "/unoptimizedIR.png").toFile());
        ImageModel model = new ImageModel(src);
        ImagePanel iPanel = new ImagePanel(model);
        OutputUnoptimizedIR.removeAll();
        OutputUnoptimizedIR.add(iPanel, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> {
            iPanel.btnFitWidth.doClick();
            model.render();
        });
    }
    
    public void handleClick() {
        try {
            String txt = editor.getText();
            dir = Files.createTempDirectory("tmp").toFile();
            File tmpFile = File.createTempFile("tmp", ".c", dir);
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(txt);
            writer.close();
            
            File outFile = File.createTempFile("out", ".txt", dir);
            File errFile = File.createTempFile("err", ".txt", dir);

            String filePath = tmpFile.getAbsolutePath();
            System.out.println("File Path: " + filePath);
            int retval = new ProcessBuilder()
                    .command("clang", "-fno-discard-value-names", "-fno-show-source-location", "-O0", "-S", "-emit-llvm", "-Xclang", "-disable-O0-optnone", "-o", "unoptimized.bc", filePath)
                    .directory(dir)
                    .redirectOutput(outFile)
                    .redirectError(errFile)
                    .start()
                    .waitFor();
            if (retval != 0) {                
                throw new IOException(new String(Files.readAllBytes(errFile.toPath())));
            }
            generateLLVMGraph(0);
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG, Toast.Style.ERROR).display();
            Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (dir != null) {
                dir.delete();
                dir = null;
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSeparator1 = new javax.swing.JSeparator();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        TextEditorPanel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        OutputUnoptimizedIR = new javax.swing.JPanel();
        OptimizationsPanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemCFG = new javax.swing.JMenuItem();
        jMenuItemCFGOnly = new javax.swing.JMenuItem();
        jMenuItemDOM = new javax.swing.JMenuItem();
        jMenuItemDOMONLY = new javax.swing.JMenuItem();
        jMenuItemPOSTDOM = new javax.swing.JMenuItem();
        jMenuItemPOSTDOMONLY = new javax.swing.JMenuItem();

        jMenuItem2.setText("jMenuItem2");

        jMenuItem1.setText("jMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(0, 0, 0));

        TextEditorPanel.setLayout(new java.awt.BorderLayout());

        OutputUnoptimizedIR.setLayout(new java.awt.BorderLayout());
        jTabbedPane1.addTab("LLVM IR", OutputUnoptimizedIR);

        OptimizationsPanel.setLayout(new java.awt.BorderLayout());
        jTabbedPane1.addTab("Optimizations", OptimizationsPanel);

        jMenu1.setText("Run...");

        jMenuItemCFG.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemCFG.setText("CFG");
        jMenuItemCFG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCFGActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemCFG);

        jMenuItemCFGOnly.setText("CFG (Headers)");
        jMenuItemCFGOnly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCFGOnlyActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemCFGOnly);

        jMenuItemDOM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemDOM.setText("DOM");
        jMenuItemDOM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDOMActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemDOM);

        jMenuItemDOMONLY.setText("DOM (Headers)");
        jMenuItemDOMONLY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDOMONLYActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemDOMONLY);

        jMenuItemPOSTDOM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemPOSTDOM.setText("POSTDOM");
        jMenuItemPOSTDOM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPOSTDOMActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemPOSTDOM);

        jMenuItemPOSTDOMONLY.setText("POSTDOM (Headers)");
        jMenuItemPOSTDOMONLY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPOSTDOMONLYActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemPOSTDOMONLY);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(TextEditorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 465, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TextEditorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemCFGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCFGActionPerformed
        graphType = CFG;
        handleClick();
    }//GEN-LAST:event_jMenuItemCFGActionPerformed

    private void jMenuItemCFGOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCFGOnlyActionPerformed
        graphType = CFG_ONLY;
        handleClick();
    }//GEN-LAST:event_jMenuItemCFGOnlyActionPerformed

    private void jMenuItemDOMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDOMActionPerformed
        graphType = DOM;
        handleClick();
    }//GEN-LAST:event_jMenuItemDOMActionPerformed

    private void jMenuItemDOMONLYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDOMONLYActionPerformed
        graphType = DOM_ONLY;
        handleClick();
    }//GEN-LAST:event_jMenuItemDOMONLYActionPerformed

    private void jMenuItemPOSTDOMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPOSTDOMActionPerformed
        graphType = POSTDOM;
        handleClick();
    }//GEN-LAST:event_jMenuItemPOSTDOMActionPerformed

    private void jMenuItemPOSTDOMONLYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPOSTDOMONLYActionPerformed
        graphType = POSTDOM_ONLY;
        handleClick();
    }//GEN-LAST:event_jMenuItemPOSTDOMONLYActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        // Use first argument as 'C' file to read in from.
        if (args.length != 0) {
            try {
                defaultContents = new String(Files.readAllBytes(Paths.get(args[0])));
            } catch (IOException ex) {
                Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Editor editor = new Editor();
                for (int i = 0; i < editor.model.size(); i++) {
                    JCheckBox cbox = editor.model.elementAt(i);
                    if (cbox.getText().equals("mem2reg")) {
                        cbox.setSelected(true);
                        break;
                    }
                }
                editor.handleClick();

                editor.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel OptimizationsPanel;
    private javax.swing.JPanel OutputUnoptimizedIR;
    private javax.swing.JPanel TextEditorPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItemCFG;
    private javax.swing.JMenuItem jMenuItemCFGOnly;
    private javax.swing.JMenuItem jMenuItemDOM;
    private javax.swing.JMenuItem jMenuItemDOMONLY;
    private javax.swing.JMenuItem jMenuItemPOSTDOM;
    private javax.swing.JMenuItem jMenuItemPOSTDOMONLY;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables
}
