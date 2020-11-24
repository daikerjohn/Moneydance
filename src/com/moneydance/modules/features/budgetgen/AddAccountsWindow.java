/*
 * Copyright (c) 2014, Michael Bray. All rights reserved.
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
package com.moneydance.modules.features.budgetgen;
/*
 * Add Accounts Window - lists Expense Categories from 'missing' Map (see BudgetParameters) 
 */
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.moneydance.modules.features.mrbutil.MRBDebug;


public class AddAccountsWindow extends JPanel {
	public static int SCREENWIDTH = Constants.ADDSCREENWIDTH-30;
	public static int SCREENHEIGHT = Constants.ADDSCREENHEIGHT-100;
	private AddAccountsModel modAccounts;
	private Map<String,AccountDetails> mapAccounts;
	private BudgetParameters objParams;
	private AddAccountsTable tabAccounts;
 	public AddAccountsWindow(BudgetParameters objParamsp ) {
		objParams = objParamsp;
		/*
		 * get list of categories in the file but not in the parameters
		 */
		mapAccounts =objParams.getMissing();
		Set<String> setAccounts= mapAccounts.keySet();
		String[]arrAccounts = setAccounts.toArray(new String[0]);
		/*
		 * set up table
		 */
		tabAccounts = new AddAccountsTable(new AddAccountsModel());
		modAccounts = (AddAccountsModel) tabAccounts.getModel();
		modAccounts.setRows(arrAccounts);
		tabAccounts.setPreferredScrollableViewportSize(new Dimension(SCREENWIDTH-20,SCREENHEIGHT-20));
		JScrollPane scrAccounts = new JScrollPane(tabAccounts);
		scrAccounts.setPreferredSize(new Dimension(SCREENWIDTH, SCREENHEIGHT));
		add(scrAccounts);
		/*
		 * add "Add" button 
		 */
		JButton butAdd = new JButton("Add Selected");
		butAdd.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed(ActionEvent e) {
				addSelected();
			}
	    });
		add(butAdd);
		/*
		 * add "Close" button
		 */
		JButton butClose = new JButton("Close");
		butClose.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
	    });
		add(butClose);
		
	}
	/*
	 * functions for buttons
	 * 
	 * Add Selected - cycle through table and add selected accounts.
	 * 
	 * Add Account to list of budget lines
	 * Remove from list of missing accounts
	 */
	public void addSelected(){
		for (int i=0;i<modAccounts.getRowCount();i++) {
			if ((Boolean) modAccounts.getValueAt(i,0)) {
				Main.debugInst.debug("AddAccountsWindow","addSelected",MRBDebug.DETAILED, "Account added ="+(String)modAccounts.getValueAt(i,1));					
				objParams.addCategory((String)modAccounts.getValueAt(i,1));
			}
		}
		Collections.sort( objParams.getLines());
		objParams.setParents();
		/*
		 * mapAccounts has changed inside objParams
		 * Reload table
		 */
		Set<String> setAccounts= mapAccounts.keySet();
		String[]arrAccounts = setAccounts.toArray(new String[0]);
		modAccounts.setRows(arrAccounts);
		/*
		 * reload the screen
		 */
		modAccounts.fireTableDataChanged();
	}
	/*
	 * Close window
	 */
	public void close() {
		this.setVisible(false);
		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
		topFrame.dispose();	
	}
}