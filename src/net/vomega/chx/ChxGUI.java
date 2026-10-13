/*
 * Decompiled with CFR 0.152.
 */
package net.vomega.chx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import net.vomega.chx.ChxAccount;
import net.vomega.chx.ChxAccountNode;
import net.vomega.chx.ChxCourse;
import net.vomega.chx.ChxCourseNode;
import net.vomega.chx.ChxProcessor;
import net.vomega.chx.ChxResource;
import net.vomega.chx.ChxResourceNode;
import net.vomega.chx.ChxSection;
import net.vomega.chx.ChxSectionNode;
import net.vomega.chx.ChxUtility;

public class ChxGUI {
    static final String filePath = String.valueOf(System.getProperty("user.home")) + File.separator + "chxbin.dat";
    ChxProcessor processor = new ChxProcessor(this);
    ChxUtility chx = new ChxUtility();
    int playSpeed = 1;
    DefaultTreeModel treeModel;
    DefaultTableModel tableModel;
    JTree tree;
    JTable table;
    JButton buttonStart;
    JLabel label;

    public void run() {
        this.prepareChx();
        final JFrame jf = new JFrame("\u8d85\u661f\u5b9e\u7528\u5de5\u5177 | https://github.com/MikeWang000000/ChxUtility");
        jf.setSize(1100, 680);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(0);
        jf.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e) {
                int ret = JOptionPane.showConfirmDialog(null, "\u4fdd\u5b58\u5e10\u53f7\u4fe1\u606f\uff1f", "\u5173\u95ed\u524d\u786e\u8ba4", 0, 3);
                if (ret == 0) {
                    ChxGUI.this.chx.saveTo(filePath);
                    System.exit(0);
                } else if (ret == 1) {
                    new File(filePath).delete();
                    System.exit(0);
                }
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        JPanel subpanelLeft = new JPanel(new BorderLayout());
        JPanel subpanelRight = new JPanel(new BorderLayout());
        ChxAccount account = this.chx.getAccount();
        ChxAccountNode rootNode = new ChxAccountNode(account.toString(), account);
        rootNode.add(new DefaultMutableTreeNode("..."));
        this.treeModel = new DefaultTreeModel(rootNode);
        this.tree = new JTree(this.treeModel);
        this.tree.setShowsRootHandles(true);
        this.tree.setEditable(false);
        this.tree.collapseRow(0);
        this.tree.addTreeExpansionListener(new TreeExpansionListener(){

            @Override
            public void treeExpanded(final TreeExpansionEvent e) {
                new Thread(){

                    @Override
                    public void run() {
                        ChxGUI gui = ChxGUI.this;
                        gui.label.setText("\u6b63\u5728\u8f7d\u5165\u76ee\u5f55...");
                        TreePath p = e.getPath();
                        Object component = p.getLastPathComponent();
                        if (component instanceof ChxAccountNode) {
                            ChxGUI.this.loadCourseNodes((ChxAccountNode)component);
                        } else if (component instanceof ChxCourseNode) {
                            ChxGUI.this.loadSectionNodes((ChxCourseNode)component);
                        } else if (component instanceof ChxSectionNode) {
                            ChxGUI.this.loadResourceNodes((ChxSectionNode)component);
                        }
                        gui.label.setText("\u5c31\u7eea\u3002");
                    }
                }.start();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent e) {
            }
        });
        JScrollPane scrollPane = new JScrollPane(this.tree);
        scrollPane.setPreferredSize(new Dimension(260, 0));
        subpanelLeft.add((Component)scrollPane, "Center");
        JButton buttonAdd = new JButton("\u6dfb\u52a0\u4efb\u52a1");
        buttonAdd.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(){

                    @Override
                    public void run() {
                        ChxGUI gui = ChxGUI.this;
                        try {
                            gui.label.setText("\u6b63\u5728\u8f7d\u5165\u8d44\u6e90...");
                            TreePath[] paths = gui.tree.getSelectionPaths();
                            int i = 0;
                            while (i < paths.length) {
                                Object component = paths[i].getLastPathComponent();
                                if (component instanceof ChxResourceNode) {
                                    ChxResourceNode node = (ChxResourceNode)component;
                                    ChxResource resource = node.getResource();
                                    int rowCount = gui.tableModel.getRowCount();
                                    int j = 0;
                                    while (j < rowCount) {
                                        if (gui.tableModel.getValueAt(j, 1) == resource) {
                                            gui.label.setText("\u5c31\u7eea\u3002");
                                            return;
                                        }
                                        ++j;
                                    }
                                    try {
                                        gui.chx.loadResourceInfo(resource);
                                    }
                                    catch (IOException | InterruptedException ex) {
                                        ex.printStackTrace();
                                        ChxUtility.log("\u52a0\u8f7d\u8d44\u6e90\u5931\u8d25\uff1a" + ex);
                                    }
                                    catch (Throwable t) {
                                        t.printStackTrace();
                                        ChxUtility.log("\u52a0\u8f7d\u8d44\u6e90\u5f02\u5e38\uff1a" + t);
                                    }
                                    if (resource.supported) {
                                        gui.tableModel.addRow(new Object[]{gui.tableModel.getRowCount() + 1, resource, -1, resource.duration});
                                    } else {
                                        gui.tableModel.addRow(new Object[]{gui.tableModel.getRowCount() + 1, resource, 200, resource.duration});
                                    }
                                }
                                ++i;
                            }
                            gui.label.setText("\u5c31\u7eea\u3002");
                        }
                        catch (Throwable t) {
                            t.printStackTrace();
                            ChxUtility.log("\u6dfb\u52a0\u4efb\u52a1\u51fa\u9519\uff1a" + t);
                            gui.label.setText("\u6dfb\u52a0\u4efb\u52a1\u51fa\u9519\uff0c\u8be6\u60c5\u89c1\u65e5\u5fd7\u3002");
                        }
                    }
                }.start();
            }
        });
        subpanelLeft.add((Component)buttonAdd, "South");
        panel.add((Component)subpanelLeft, "West");
        this.tableModel = new DefaultTableModel(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(this.tableModel);
        this.tableModel.addColumn("\u5e8f\u53f7");
        this.tableModel.addColumn("\u4efb\u52a1");
        this.tableModel.addColumn("\u8fdb\u5ea6");
        this.tableModel.addColumn("\u603b\u65f6\u957f");
        TableColumn column = this.table.getColumnModel().getColumn(0);
        column.setMaxWidth(50);
        column.setPreferredWidth(50);
        column = this.table.getColumnModel().getColumn(2);
        column.setCellRenderer(new DefaultTableCellRenderer(){
            private static final long serialVersionUID = 1L;
            private final JProgressBar bar = new JProgressBar(0, 100);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String text;
                Integer progress = (Integer)value;
                if (progress == -100) {
                    String string = "\u670d\u52a1\u5668\u8fd4\u56de\u9519\u8bef\u3002";
                }
                if (progress == 200) {
                    text = "\u4e0d\u9002\u7528";
                } else if (progress < 0) {
                    text = "\u672a\u5f00\u59cb";
                } else {
                    if (progress < 100) {
                        this.bar.setValue(progress);
                        return this.bar;
                    }
                    text = "\u5df2\u5b8c\u6210";
                }
                return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
            }
        });
        column = this.table.getColumnModel().getColumn(3);
        column.setMaxWidth(100);
        column.setPreferredWidth(100);
        column.setCellRenderer(new DefaultTableCellRenderer(){
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String text = "";
                int duration = value == null ? 0 : (value instanceof String ? Integer.valueOf((String)value).intValue() : ((Integer)value).intValue());
                int hour = duration / 3600;
                int minute = (duration - hour * 3600) / 60;
                int second = duration - hour * 3600 - minute * 60;
                text = String.format("%d:%02d:%02d", hour, minute, second);
                return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
            }
        });
        JScrollPane scrollPane2 = new JScrollPane(this.table);
        subpanelRight.add((Component)scrollPane2, "Center");
        this.label = new JLabel("\u5c31\u7eea\u3002");
        this.label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        JButton buttonSpeed = new JButton("\u8bbe\u7f6e\u901f\u7387");
        buttonSpeed.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ChxGUI gui = ChxGUI.this;
                JOptionPane.showMessageDialog(jf, "\u66f4\u6539\u901f\u7387\u6709\u5c01\u7981\u98ce\u9669\uff0c\u8bf7\u914c\u60c5\u8bbe\u7f6e\u3002", "\u8b66\u544a", 2);
                JComboBox<String> cmb = new JComboBox<String>();
                cmb.addItem("1x");
                cmb.addItem("2x");
                cmb.addItem("4x");
                cmb.addItem("\u6700\u5feb");
                if (JOptionPane.showConfirmDialog(null, cmb, "\u66f4\u6539\u901f\u7387", 2, 3) == 0) {
                    String spx = (String)cmb.getSelectedItem();
                    if (spx.equals("1x")) {
                        gui.playSpeed = 1;
                    } else if (spx.equals("2x")) {
                        gui.playSpeed = 2;
                    } else if (spx.equals("4x")) {
                        gui.playSpeed = 4;
                    } else if (spx.equals("\u6700\u5feb")) {
                        gui.playSpeed = -1;
                    }
                }
            }
        });
        JButton buttonDownload = new JButton("\u4e0b\u8f7d\u8d44\u6e90");
        buttonDownload.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ChxGUI gui = ChxGUI.this;
                int row = gui.table.getSelectedRow();
                if (row != -1) {
                    ChxResource resource = (ChxResource)gui.tableModel.getValueAt(row, 1);
                    if (Desktop.isDesktopSupported()) {
                        Desktop dp = Desktop.getDesktop();
                        if (dp.isSupported(Desktop.Action.BROWSE)) {
                            try {
                                dp.browse(URI.create(resource.downloadurl));
                                return;
                            }
                            catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                        JOptionPane.showMessageDialog(jf, "\u60a8\u7684\u7cfb\u7edf\u53ef\u80fd\u4e0d\u652f\u6301\u6b64\u9879\u529f\u80fd\u3002", "\u6d88\u606f", 1);
                    }
                } else {
                    JOptionPane.showMessageDialog(jf, "\u60a8\u6ca1\u6709\u9009\u4e2d\u4efb\u4f55\u884c\u3002", "\u6d88\u606f", 1);
                }
            }
        });
        JButton buttonClear = new JButton("\u6e05\u7a7a\u4efb\u52a1");
        buttonClear.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ChxGUI gui = ChxGUI.this;
                if (gui.processor.isRunning()) {
                    JOptionPane.showMessageDialog(jf, "\u4efb\u52a1\u6b63\u5728\u8fd0\u884c\u4e2d\uff01", "\u8b66\u544a", 2);
                } else {
                    int i = gui.tableModel.getRowCount() - 1;
                    while (i >= 0) {
                        gui.tableModel.removeRow(i);
                        --i;
                    }
                }
            }
        });
        JButton buttonDelete = new JButton("\u5220\u9664\u4efb\u52a1");
        buttonDelete.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ChxGUI gui = ChxGUI.this;
                if (gui.processor.isRunning()) {
                    JOptionPane.showMessageDialog(jf, "\u4efb\u52a1\u6b63\u5728\u8fd0\u884c\u4e2d\uff01", "\u8b66\u544a", 2);
                } else {
                    int[] rows = gui.table.getSelectedRows();
                    int i = rows.length - 1;
                    while (i >= 0) {
                        gui.tableModel.removeRow(rows[i]);
                        --i;
                    }
                    i = gui.tableModel.getRowCount() - 1;
                    while (i >= 0) {
                        gui.tableModel.setValueAt(i + 1, i, 0);
                        --i;
                    }
                }
            }
        });
        this.buttonStart = new JButton("\u5168\u90e8\u5f00\u59cb");
        this.buttonStart.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ChxGUI gui = ChxGUI.this;
                if (!gui.processor.isRunning()) {
                    gui.processor.start();
                    gui.buttonStart.setText("\u5168\u90e8\u6682\u505c");
                } else {
                    gui.processor.stop();
                    gui.buttonStart.setText("\u5168\u90e8\u5f00\u59cb");
                }
            }
        });
        JPanel rightButtonsPanel = new JPanel();
        rightButtonsPanel.setLayout(new GridLayout(1, 4));
        JPanel functionPanel = new JPanel();
        functionPanel.setLayout(new GridLayout(1, 2));
        functionPanel.add(buttonSpeed);
        functionPanel.add(buttonDownload);
        JPanel removePanel = new JPanel();
        removePanel.setLayout(new GridLayout(1, 2));
        removePanel.add(buttonClear);
        removePanel.add(buttonDelete);
        rightButtonsPanel.add(this.label);
        rightButtonsPanel.add(functionPanel);
        rightButtonsPanel.add(removePanel);
        rightButtonsPanel.add(this.buttonStart);
        subpanelRight.add((Component)rightButtonsPanel, "South");
        panel.add((Component)subpanelRight, "Center");
        jf.setContentPane(panel);
        jf.setVisible(true);
    }

    public void prepareChx() {
        try {
            if (this.chx.loadFrom(filePath) && (this.chx.checkLogin() || this.chx.reLogin() != null)) {
                return;
            }
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        String username = "";
        String password = "";
        JTextField tf = new JTextField();
        if (JOptionPane.showConfirmDialog(null, tf, "\u8bf7\u8f93\u5165\u8d85\u661f\u5e10\u53f7", 2, 1) == 0) {
            username = tf.getText();
        } else {
            System.exit(0);
        }
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(null, pf, "\u8bf7\u8f93\u5165\u5bc6\u7801", 2, 1) == 0) {
            password = new String(pf.getPassword());
        } else {
            System.exit(0);
        }
        try {
            if (this.chx.login(username, password) != null) {
                return;
            }
            JOptionPane.showMessageDialog(null, "\u767b\u5f55\u5931\u8d25\uff01\u8bf7\u68c0\u67e5\u5e10\u53f7\u4fe1\u606f\u3002", "\u8b66\u544a", 2);
            System.exit(0);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
            ChxUtility.log("\u767b\u5f55\u8fc7\u7a0b\u51fa\u9519\uff1a" + e);
            String detail = e.getMessage() == null ? String.valueOf(e) : e.getMessage();
            JOptionPane.showMessageDialog(null, "\u767b\u5f55\u5931\u8d25\uff01\n" + detail + "\n\n\uff08\u8be6\u7ec6\u4fe1\u606f\u89c1 " + ChxUtility.logPath + "\uff09", "\u8b66\u544a", 2);
            System.exit(0);
        }
    }

    private void loadCourseNodes(ChxAccountNode node) {
        try {
            node.removeAllChildren();
            ChxCourse[] courses = this.chx.getCourses();
            int i = 0;
            while (i < courses.length) {
                ChxCourse course = courses[i];
                if (course.supported) {
                    ChxCourseNode courseNode = new ChxCourseNode(course.toString(), course);
                    courseNode.add(new DefaultMutableTreeNode("..."));
                    node.add(courseNode);
                }
                ++i;
            }
            if (node.getChildCount() == 0) {
                node.add(new DefaultMutableTreeNode("(\u7a7a)"));
            }
        }
        catch (IOException | InterruptedException e) {
            node.add(new DefaultMutableTreeNode("(\u9519\u8bef)"));
            ChxUtility.log("加载目录失败：" + e);
            e.printStackTrace();
        }
        catch (Throwable t) {
            node.add(new DefaultMutableTreeNode("(\u9519\u8bef)"));
            ChxUtility.log("加载目录异常：" + t);
            t.printStackTrace();
        }
        this.treeModel.nodeStructureChanged(node);
    }

    private void loadSectionNodes(ChxCourseNode node) {
        node.removeAllChildren();
        try {
            node.removeAllChildren();
            ChxSection[] sections = this.chx.getSections(node.getCourse());
            int i = 0;
            while (i < sections.length) {
                ChxSection section = sections[i];
                if (section.supported) {
                    ChxSectionNode sectionNode = new ChxSectionNode(section.toString(), section);
                    sectionNode.add(new DefaultMutableTreeNode("..."));
                    node.add(sectionNode);
                    if (node.getChildCount() == 0) {
                        node.add(new DefaultMutableTreeNode("(\u7a7a)"));
                    }
                }
                ++i;
            }
        }
        catch (IOException | InterruptedException e) {
            node.add(new DefaultMutableTreeNode("(\u9519\u8bef)"));
            ChxUtility.log("加载目录失败：" + e);
            e.printStackTrace();
        }
        catch (Throwable t) {
            node.add(new DefaultMutableTreeNode("(\u9519\u8bef)"));
            ChxUtility.log("加载目录异常：" + t);
            t.printStackTrace();
        }
        this.treeModel.nodeStructureChanged(node);
    }

    private void loadResourceNodes(ChxSectionNode node) {
        node.removeAllChildren();
        try {
            node.removeAllChildren();
            ChxResource[] resources = this.chx.getResources(node.getSection());
            int i = 0;
            while (i < resources.length) {
                ChxResource resource = resources[i];
                if (resource.supported || resource.name != null) {
                    ChxResourceNode resourceNode = new ChxResourceNode(resource.toString(), resource);
                    node.add(resourceNode);
                }
                ++i;
            }
            if (node.getChildCount() == 0) {
                node.add(new DefaultMutableTreeNode("(\u7a7a)"));
            }
        }
        catch (IOException | InterruptedException e) {
            node.add(new DefaultMutableTreeNode("(\u9519\u8bef)"));
            ChxUtility.log("加载资料失败：" + e);
            e.printStackTrace();
        }
        catch (Throwable t) {
            node.add(new DefaultMutableTreeNode("(\u9519\u8bef)"));
            ChxUtility.log("加载资料异常：" + t);
            t.printStackTrace();
        }
        this.treeModel.nodeStructureChanged(node);
    }

    public static void main(String[] args) {
        try {
            ChxGUI self = new ChxGUI();
            self.run();
        }
        catch (Throwable t) {
            t.printStackTrace();
            ChxUtility.log("\u7a0b\u5e8f\u5f02\u5e38\u7ec8\u6b62\uff1a" + t);
            JOptionPane.showMessageDialog(null, "\u7a0b\u5e8f\u53d1\u751f\u9519\u8bef\uff1a\n" + t + "\n\n\u8be6\u7ec6\u4fe1\u606f\u89c1\u65e5\u5fd7\uff1a\n" + ChxUtility.logPath, "\u9519\u8bef", 0);
            System.exit(0);
        }
    }
}


class ChxAccountNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = 524269464L;
	private String name;
	private ChxAccount account;
	
	public ChxAccountNode(String name, ChxAccount account)
	{
		super(name);
		this.name = name;
		this.account = account;
	}
	public String getName()
	{
		return this.name;
	}
	public ChxAccount getAccount()
	{
		return this.account;
	}
}

class ChxCourseNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = 524269464L;
	private String name;
	private ChxCourse course;
	
	public ChxCourseNode(String name, ChxCourse course)
	{
		super(name);
		this.name = name;
		this.course = course;
	}
	public String getName()
	{
		return this.name;
	}
	public ChxCourse getCourse()
	{
		return this.course;
	}
}

class ChxSectionNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = 524269464L;
	private String name;
	private ChxSection section;
	
	public ChxSectionNode(String name, ChxSection section)
	{
		super(name);
		this.name = name;
		this.section = section;
	}
	public String getName()
	{
		return this.name;
	}
	public ChxSection getSection()
	{
		return this.section;
	}
}

class ChxResourceNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = 524269464L;
	private String name;
	private ChxResource resource;
	
	public ChxResourceNode(String name, ChxResource resource)
	{
		super(name);
		this.name = name;
		this.resource = resource;
	}
	public String getName()
	{
		return this.name;
	}
	public ChxResource getResource()
	{
		return this.resource;
	}
}
