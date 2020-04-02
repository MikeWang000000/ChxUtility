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
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class ChxGUI
{
	static final String filePath = System.getProperty("user.home") + File.separator + "chxbin.dat";
	ChxProcessor processor = new ChxProcessor(this);
	ChxUtility chx = new ChxUtility();
	int playSpeed = 1;
	DefaultTreeModel treeModel;
	DefaultTableModel tableModel;
	JTree tree;
	JTable table;
	JButton buttonStart;
	JLabel label;
	
	public void run()
	{
		prepareChx();
		JFrame jf = new JFrame("超星实用工具 | https://github.com/MikeWang000000/ChxUtility");
		jf.setSize(1100, 680);
		jf.setLocationRelativeTo(null);
		jf.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		jf.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				int ret = JOptionPane.showConfirmDialog(null, "保存帐号信息？", "关闭前确认", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (ret == JOptionPane.YES_OPTION)
				{
					ChxGUI.this.chx.saveTo(filePath);
					System.exit(0);
				}
				else if (ret == JOptionPane.NO_OPTION)
				{
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
		
		this.treeModel=new DefaultTreeModel(rootNode);
		this.tree = new JTree(this.treeModel);
		this.tree.setShowsRootHandles(true);
		this.tree.setEditable(false);
		this.tree.collapseRow(0);

		this.tree.addTreeExpansionListener(new TreeExpansionListener()
		{
			@Override
			public void treeExpanded(TreeExpansionEvent e)
			{
				// this action may cost a bit of time. use thread.
				(new Thread()
				{
					@Override
					public void run() 
					{
						ChxGUI gui = ChxGUI.this;
						gui.label.setText("正在载入目录...");
						TreePath p = e.getPath();
						Object component = p.getLastPathComponent();
						
						if (component instanceof ChxAccountNode)
							loadCourseNodes((ChxAccountNode) component);
						else if (component instanceof ChxCourseNode)
							loadSectionNodes((ChxCourseNode) component);
						else if (component instanceof ChxSectionNode)
							loadResourceNodes((ChxSectionNode) component);
						gui.label.setText("就绪。");
            synchronized (ChxGUI.this) {
                ChxGUI.this.notify();
            }
				  }
				}).start();
			}
			@Override
			public void treeCollapsed(TreeExpansionEvent e)
			{
				// do nothing when collapsed
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(this.tree);
		scrollPane.setPreferredSize(new Dimension(260, 0));
		subpanelLeft.add(scrollPane, BorderLayout.CENTER);
		
		JButton buttonAdd = new JButton("添加任务");
		buttonAdd.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// this action may cost a bit of time. use thread.
				(new Thread()
				{
					@Override
					public void run() 
					{
						ChxGUI gui = ChxGUI.this;
						gui.label.setText("正在载入资源...");
						TreePath[] paths = gui.tree.getSelectionPaths();
						for (int i=0; i<paths.length; i++)
						{
							Object component = paths[i].getLastPathComponent();
							// ignore components which are not ChxResourceNode (e.g. DefaultMutableTreeNode)
							if (component instanceof ChxResourceNode)
							{
								ChxResourceNode node = (ChxResourceNode) component;
								ChxResource resource = node.getResource();
								
								// if added, do nothing.
								int rowCount = gui.tableModel.getRowCount();
								for (int j=0; j<rowCount; j++)
								{
									if (gui.tableModel.getValueAt(j, 1) == resource)
										return;
								}
								
								// detailed resource information should be loaded, such as token
								try { gui.chx.loadResourceInfo(resource); }
								catch (IOException | InterruptedException ex) { ex.printStackTrace(); }
								
								if (resource.supported)
									gui.tableModel.addRow(new Object[]{gui.tableModel.getRowCount()+1, resource, -1, resource.duration});
								else
									gui.tableModel.addRow(new Object[]{gui.tableModel.getRowCount()+1, resource, 200, resource.duration});
							}
						}
						gui.label.setText("就绪。");
				    }
				}).start();
			}
		});
    JPanel allPanel = new JPanel();
    allPanel.add(buttonAdd);
    JButton allButton = new JButton("全部展开");

    allButton.addActionListener(e -> new Thread(() -> {
                ChxGUI gui = ChxGUI.this;
                gui.label.setText("请不要操作 稍等一会");
                gui.tree.setEnabled(false);
                synchronized (ChxGUI.this) {
                    try {
                        gui.tree.expandRow(0);
                        Thread.sleep((int) (1000 * Math.random()));
                        ChxGUI.this.wait();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }

                TreeNode root = (TreeNode) gui.treeModel.getRoot();
                for (int i = root.getChildCount() - 1; i >= 0; i--) {
                    TreeNode course = root.getChildAt(i);
                    synchronized (ChxGUI.this) {
                        try {
                            gui.tree.expandPath(ChxUtility.getPath(course));
                            Thread.sleep((int) (1000 * Math.random()));
                            ChxGUI.this.wait();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }

                    for (int j = course.getChildCount() - 1; j >= 0; j--) {
                        TreeNode lesson = course.getChildAt(j);
                        synchronized (ChxGUI.this) {
                            try {
                                gui.tree.expandPath(ChxUtility.getPath(lesson));
                                Thread.sleep((int) (2000 * Math.random()));
                                ChxGUI.this.wait();
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
                gui.tree.setEnabled(true);
                gui.label.setText("就绪。");
            }).start()
    );

    allPanel.add(allButton);
    subpanelLeft.add(allPanel, BorderLayout.SOUTH);
		panel.add(subpanelLeft, BorderLayout.WEST);
		
		// Right part
		
		this.tableModel = new DefaultTableModel()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		this.table = new JTable(this.tableModel);
		this.tableModel.addColumn("序号");
		this.tableModel.addColumn("任务");
		this.tableModel.addColumn("进度");
		this.tableModel.addColumn("总时长");
		
		TableColumn column;
		// column "序号"
		column = this.table.getColumnModel().getColumn(0);
		column.setMaxWidth(50);
		column.setPreferredWidth(50);
		
		// column "进度"
		column = this.table.getColumnModel().getColumn(2);
		column.setCellRenderer(new DefaultTableCellRenderer()
		{
			private static final long serialVersionUID = 1L;
			private final JProgressBar bar = new JProgressBar(0, 100);
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				Integer progress = (Integer) value;
				String text;
				// specific values: -100, 200
				if (progress == -100)
					text = "服务器返回错误。";
				if (progress == 200)
					text = "不适用";
				else if (progress < 0)
					text = "未开始";
				else if (progress < 100)
				{
					this.bar.setValue(progress);
					return this.bar;
				}
				else
					text = "已完成";
				return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
			}
		});
		
		// column "总时长"
		column = this.table.getColumnModel().getColumn(3);
		column.setMaxWidth(100);
		column.setPreferredWidth(100);
		// time formatting
		column.setCellRenderer(new DefaultTableCellRenderer()
		{
			private static final long serialVersionUID = 1L;
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				String text = "";
				int duration;
				
				if (value == null)
					duration = 0;
				else if (value instanceof String)
					duration = Integer.valueOf((String) value);
				else
					duration = (Integer) value;

				int hour = duration / 3600;
				int minute = (duration - hour * 3600) / 60;
				int second = (duration - hour * 3600 - minute * 60);
				
				text = String.format("%d:%02d:%02d", hour, minute, second);
				return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
			}
		});
		
		JScrollPane scrollPane2 = new JScrollPane(this.table);
		subpanelRight.add(scrollPane2, BorderLayout.CENTER);
		
		this.label = new JLabel("就绪。");
		this.label.setBorder(BorderFactory.createEmptyBorder(5,10,5,5));
		
		JButton buttonSpeed = new JButton("设置速率");
		buttonSpeed.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ChxGUI gui = ChxGUI.this;
				JOptionPane.showMessageDialog(jf, "更改速率有封禁风险，请酌情设置。", "警告", JOptionPane.WARNING_MESSAGE);
				JComboBox<String> cmb = new JComboBox<>();
		        cmb.addItem("1x");
		        cmb.addItem("2x");
		        cmb.addItem("4x");
		        cmb.addItem("最快");
		        if (JOptionPane.showConfirmDialog(null, cmb, "更改速率", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION)
		        {
		        	String spx = (String) cmb.getSelectedItem();
		        	if (spx.equals("1x"))
		        		gui.playSpeed = 1;
		        	else if (spx.equals("2x"))
		        		gui.playSpeed = 2;
		        	else if (spx.equals("4x"))
		        		gui.playSpeed = 4;
		        	else if (spx.equals("最快"))
		        		gui.playSpeed = -1;
		        }
			}
		});
		
		JButton buttonDownload = new JButton("下载资源");
		buttonDownload.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ChxGUI gui = ChxGUI.this;
				int row = gui.table.getSelectedRow();
				if (row != -1)
				{
					ChxResource resource = (ChxResource) gui.tableModel.getValueAt(row, 1);
					if (Desktop.isDesktopSupported())
					{
						Desktop dp = Desktop.getDesktop();
						if (dp.isSupported(Desktop.Action.BROWSE))
							try
							{
								dp.browse(URI.create(resource.downloadurl));
								return;
							}
							catch (IOException ex)
							{
								ex.printStackTrace();
							}
						JOptionPane.showMessageDialog(jf, "您的系统可能不支持此项功能。", "消息", JOptionPane.INFORMATION_MESSAGE);
					}
				}
				else
					JOptionPane.showMessageDialog(jf, "您没有选中任何行。", "消息", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		JButton buttonClear = new JButton("清空任务");
		buttonClear.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ChxGUI gui = ChxGUI.this;
				if (gui.processor.isRunning())
					JOptionPane.showMessageDialog(jf, "任务正在运行中！", "警告", JOptionPane.WARNING_MESSAGE);
				else
					// deleting lines will affect subsequent lines, thus traverse them by inverted order
					for (int i=gui.tableModel.getRowCount()-1; i>=0; i--)
					{
						gui.tableModel.removeRow(i);
					}
			}
		});
		
		JButton buttonDelete = new JButton("删除任务");
		buttonDelete.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ChxGUI gui = ChxGUI.this;
				if (gui.processor.isRunning())
					JOptionPane.showMessageDialog(jf, "任务正在运行中！", "警告", JOptionPane.WARNING_MESSAGE);
				else
				{
					int[] rows = gui.table.getSelectedRows();
					// deleting lines will affect subsequent lines, thus traverse them by inverted order
					for (int i=rows.length-1; i>=0; i--)
					{
						gui.tableModel.removeRow(rows[i]);
					}
					// update row number
					for (int i=gui.tableModel.getRowCount()-1; i>=0; i--)
					{
						gui.tableModel.setValueAt(i+1, i, 0);
					}
				}
			}
		});
		
		this.buttonStart = new JButton("全部开始");
		this.buttonStart.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ChxGUI gui = ChxGUI.this;
				if (!gui.processor.isRunning())
				{
					gui.processor.start();
					gui.buttonStart.setText("全部暂停");
				}
				else
				{
					gui.processor.stop();
					gui.buttonStart.setText("全部开始");
				}
			}
		});
		
		JPanel rightButtonsPanel = new JPanel();
		rightButtonsPanel.setLayout(new GridLayout(1,4));
		
		JPanel functionPanel = new JPanel();
		functionPanel.setLayout(new GridLayout(1,2));
		functionPanel.add(buttonSpeed);
		functionPanel.add(buttonDownload);
		
		JPanel removePanel = new JPanel();
		removePanel.setLayout(new GridLayout(1,2));
		removePanel.add(buttonClear);
		removePanel.add(buttonDelete);
		
		rightButtonsPanel.add(this.label);
		rightButtonsPanel.add(functionPanel);
		rightButtonsPanel.add(removePanel);
		rightButtonsPanel.add(this.buttonStart);
		
		subpanelRight.add(rightButtonsPanel, BorderLayout.SOUTH);
		panel.add(subpanelRight, BorderLayout.CENTER);
		
		jf.setContentPane(panel);
		jf.setVisible(true);
	}
	
	public void prepareChx()
	{
		try
		{
			// try to load account info from local file
			if (this.chx.loadFrom(filePath))
				// if saved login info is correct, skip login procedure
				if (this.chx.checkLogin() || this.chx.reLogin() != null)
					return;
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
		
		String username = "";
		String password = "";
		
		JTextField tf = new JTextField();
		if (JOptionPane.showConfirmDialog(null, tf, "请输入超星帐号", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION)
			username = tf.getText();
		else
			System.exit(0);
		
		JPasswordField pf = new JPasswordField();
		if (JOptionPane.showConfirmDialog(null, pf, "请输入密码", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION)
			password = new String(pf.getPassword());
		else
			System.exit(0);
		
		try
		{
			if (this.chx.login(username, password) != null)
			{
				return;
			}
			else
			{
				JOptionPane.showMessageDialog(null, "登录失败！请检查帐号信息。", "警告", JOptionPane.WARNING_MESSAGE);
				System.exit(0);
			}
		}
		catch (IOException | InterruptedException e)
		{
			JOptionPane.showMessageDialog(null, "登录失败！请检查网络连接。", "警告", JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void loadCourseNodes(ChxAccountNode node)
	{
		try
		{
			node.removeAllChildren();
			ChxCourse[] courses = this.chx.getCourses();
			for (int i=0; i<courses.length; i++)
			{
				ChxCourse course = courses[i];
				if (!course.supported)
					continue;
				ChxCourseNode courseNode = new ChxCourseNode(course.toString(), course);
				courseNode.add(new DefaultMutableTreeNode("..."));
				node.add(courseNode);
			}
			if (node.getChildCount() == 0)
				node.add(new DefaultMutableTreeNode("(空)"));
		}
		catch (IOException | InterruptedException e)
		{
			node.add(new DefaultMutableTreeNode("(错误)"));
			e.printStackTrace();
		}
		this.treeModel.nodeStructureChanged(node);
	}
	
	private void loadSectionNodes(ChxCourseNode node)
	{
		node.removeAllChildren();
		try
		{
			node.removeAllChildren();
			ChxSection[] sections = this.chx.getSections(node.getCourse());
			for (int i=0; i<sections.length; i++)
			{
				ChxSection section = sections[i];
				if (!section.supported)
					continue;
				ChxSectionNode sectionNode = new ChxSectionNode(section.toString(), section);
				sectionNode.add(new DefaultMutableTreeNode("..."));
				node.add(sectionNode);
				if (node.getChildCount() == 0)
					node.add(new DefaultMutableTreeNode("(空)"));
			}
		}
		catch (IOException | InterruptedException e)
		{
			node.add(new DefaultMutableTreeNode("(错误)"));
			e.printStackTrace();
		}
		this.treeModel.nodeStructureChanged(node);
	}
	
	private void loadResourceNodes(ChxSectionNode node)
	{
		node.removeAllChildren();
		try
		{
			node.removeAllChildren();
			ChxResource[] resources = this.chx.getResources(node.getSection());
			for (int i=0; i<resources.length; i++)
			{
				ChxResource resource = resources[i];
				if (!resource.supported && resource.name == null)
					continue;
				ChxResourceNode resourceNode = new ChxResourceNode(resource.toString(), resource);
				node.add(resourceNode);
			}
			if (node.getChildCount() == 0)
				node.add(new DefaultMutableTreeNode("(空)"));
		}
		catch (IOException | InterruptedException e)
		{
			node.add(new DefaultMutableTreeNode("(错误)"));
			e.printStackTrace();
		}
		this.treeModel.nodeStructureChanged(node);
	}
	
	public static void main(String[] args)
	{
		ChxGUI self = new ChxGUI();
		self.run();
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
