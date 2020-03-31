package net.vomega.chx;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.table.TableModel;

public class ChxProcessor implements Runnable
{
	private Thread t;
	private ChxGUI gui;
	
	ChxProcessor(ChxGUI gui)
	{
		this.gui = gui;
	}
	
	@Override
	public void run()
	{
		try
		{
			this.gui.label.setText("执行中...");
			TableModel tableModel = this.gui.table.getModel();
			for (int i=0; i<tableModel.getRowCount(); i++)
			{
				ChxResource resource = (ChxResource) tableModel.getValueAt(i, 1);
				int startSec = 0;
				
				// load duration of task
				int seconds;
				if (resource.duration == null || resource.duration.isEmpty())
					seconds = 0;
				else
					seconds = Integer.valueOf(resource.duration);
				
				// continue the progress if possible
				int progressOri = (int) tableModel.getValueAt(i, 2);
				if (progressOri >= 100)
					continue; // skip completed task. 
				else if (progressOri > 0)
					startSec = progressOri * seconds / 100;
				
				boolean serverStat = false;
				if (this.gui.playSpeed > 0 && seconds != 0)
					for (int sec=startSec; sec<=seconds; sec++)
					{
						tableModel.setValueAt(100 * sec / seconds, i, 2);
						this.gui.label.setText(String.format("执行中... 当前项: %.2f%%", 100.00 * sec / seconds));
						if (sec % 10 == 0)
							try
							{
								// if server return true, finish the task
								serverStat = this.gui.chx.playResource(resource, sec);
								if (serverStat)
									break;
							}
							catch (IOException e)
							{
								serverStat = false;
								e.printStackTrace();
							}
						Thread.sleep(1000 / this.gui.playSpeed);
					}
				else
				{
					try
					{
						// complete the task immediately
						serverStat = this.gui.chx.playResource(resource, seconds);
					}
					catch (IOException e)
					{
						serverStat = false;
						e.printStackTrace();
					}
				}
				// if the last value returned by server is not true, mark it as an error
				if (serverStat)
					tableModel.setValueAt(100, i, 2);
				else
					tableModel.setValueAt(-100, i, 2);
				
			}
			JOptionPane.showMessageDialog(null, "任务全部完成。", "消息", JOptionPane.INFORMATION_MESSAGE);
			this.gui.buttonStart.setText("全部开始");
			this.gui.label.setText("就绪。");
		}
		catch (InterruptedException e)
		{
			// just stop it
		}
		this.t = null;
		System.gc();
	}
	
	public boolean isRunning()
	{
		return this.t != null;
	}
	
	public void start()
	{
		if (this.t == null)
		{
			this.t = new Thread(this);
			this.t.start();
		}
	}
	
	public void stop()
	{
		this.t.interrupt();
	}
}