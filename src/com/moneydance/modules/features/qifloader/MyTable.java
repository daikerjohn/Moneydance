/*
 *  Copyright (c) 2014, 2016, Michael Bray. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - The name of the author may not used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 
package com.moneydance.modules.features.qifloader;


import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;


public class MyTable extends JTable {
     

	private MyCheckBox boxSelect = new MyCheckBox();
	private class RenderSelect extends JComponent implements TableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
				MyCheckBox boxTemp = new MyCheckBox();
				boxTemp.setSelected((boolean)value);
				return boxTemp;
		}
		
	}
	RenderSelect objRender = new RenderSelect();
	public MyTable(TableModel dm) {
		super(dm);
		this.setFillsViewportHeight(true);
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setAutoResizeMode(AUTO_RESIZE_OFF);
		/*
		 * Select
		 */
		TableColumn colSelect = this.getColumnModel().getColumn(0);
		colSelect.setCellRenderer(objRender);
		colSelect.setCellEditor(new DefaultCellEditor(boxSelect));
		colSelect.setPreferredWidth(40);
		/*
		 * Date
		 */
		this.getColumnModel().getColumn(1).setResizable(false);
		this.getColumnModel().getColumn(1).setPreferredWidth(80);
		/*
		 * Description
		 */
		this.getColumnModel().getColumn(2).setResizable(false);
		this.getColumnModel().getColumn(2).setPreferredWidth(350);
		/*
		 * Value
		 */
		this.getColumnModel().getColumn(3).setResizable(false);
		this.getColumnModel().getColumn(3).setPreferredWidth(80);
		/*
		 * Cheque
		 */
		this.getColumnModel().getColumn(4).setResizable(false);
		this.getColumnModel().getColumn(4).setPreferredWidth(80);
		/*
		 * Category
		 */
		this.getColumnModel().getColumn(5).setResizable(false);
		this.getColumnModel().getColumn(5).setPreferredWidth(80);
		/*
		 * Status
		 */
		this.getColumnModel().getColumn(6).setResizable(false);
		this.getColumnModel().getColumn(6).setPreferredWidth(40);
	}


}
