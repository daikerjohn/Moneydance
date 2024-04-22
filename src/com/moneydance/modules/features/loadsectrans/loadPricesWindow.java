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
package com.moneydance.modules.features.loadsectrans;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.infinitekind.moneydance.model.Account;
import com.moneydance.awt.GridC;
import com.moneydance.awt.JDateField;

public class loadPricesWindow extends JPanel implements TableListener {
    private SortedSet<SecLine> setLine;
    private Account acct;
    private MyTableModel pricesModel;
    private MyTable pricesTab;
    private GenerateWindow generateWindow;
    private Parameters params;
    JScrollPane spPrices;
    JPanel panBot;
    JPanel panTop;
    JPanel panMid;
    JButton btnClose;
    JButton btnGenerate;
    JCheckBox jcSelect;
    JTextField txtAccount;
	public loadPricesWindow(JTextField txtFileName,Account acctp, Parameters objParmsp) {
		setLine = new TreeSet<SecLine>(new SecLineCompare());
		acct = acctp;
		params = objParmsp;
		
		loadFile (txtFileName);
		pricesModel = new MyTableModel (setLine, Main.mapAccounts);
		pricesTab = new MyTable (pricesModel);
		/*
		 * Start of screen
		 * 
		 * Top Panel Account
		 */
		this.setLayout(new BorderLayout());
		panTop = new JPanel (new GridBagLayout());
		int x=0;
		int y=0;
		JLabel lbAccount = new JLabel("Investment Account:");
		panTop.add(lbAccount,GridC.getc(x,y));
		x++;
		txtAccount = new JTextField(acct.getAccountName());
		panTop.add(txtAccount,GridC.getc(x,y));
		this.add(panTop,BorderLayout.PAGE_START);
		/*
		 * Middle Panel table
		 */
		panMid = new JPanel ();
		panMid.setLayout(new BoxLayout(panMid,BoxLayout.Y_AXIS));
		spPrices = new JScrollPane (pricesTab);
		spPrices.setAlignmentX(LEFT_ALIGNMENT);
		panMid.add(spPrices,BorderLayout.LINE_START);
		spPrices.setPreferredSize(new Dimension(Constants.LOADSCREENWIDTH,Constants.LOADSCREENHEIGHT));
		jcSelect = new JCheckBox("Select All");
		jcSelect.setAlignmentX(LEFT_ALIGNMENT);
		jcSelect.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean bNewValue;
				if (e.getStateChange() == ItemEvent.DESELECTED)
					bNewValue = false;
				else
					bNewValue = true;
				for (int i=0;i<pricesModel.getRowCount();i++)
					pricesModel.setValueAt(bNewValue, i, 0);
				pricesModel.fireTableDataChanged();
			}
		});
		panMid.add(jcSelect);
		this.add(panMid,BorderLayout.CENTER);		
		/*
		 * Add Buttons
		 */
		panBot = new JPanel(new GridBagLayout());
		/*
		 * Button 1
		 */
		x=0;
		y=0;
		btnClose = new JButton("Close");
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		panBot.add(btnClose,GridC.getc(x,y).west().insets(15,15,15,15));

		/*
		 * Button 2
		 */
		x++;
		btnGenerate = new JButton("Generate Transactions");
		btnGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				generate();
			}
		});
		panBot.add(btnGenerate,GridC.getc(x,y).insets(15,15,15,15));
		
		this.add(panBot,BorderLayout.PAGE_END);
		

	}

	 /*
	  * try to load selected file
	  */
	 private void loadFile(JTextField txtFileName) {
		 	@SuppressWarnings("unused")
			String strExchange;
			String strTicker; 
		 	Main.generatedTranSet = new MyTransactionSet(Main.root, acct,params,setLine);
		 	Main.generatedTranSet.addListener(this);
			try {
				FileReader frPrices = new FileReader(txtFileName.getText());
				BufferedReader brPrices = new BufferedReader(frPrices);
				/*
				 * Get the headers
				 */
				String strLine = brPrices.readLine(); 
				String [] arColumns = strLine.split(",");
				for(int i=0; i<arColumns.length; i++) {
					arColumns[i] = arColumns[i].trim();
				}
				int iDate = 0;
				int iRef = 0;
				int iDesc = 0;
				int iTicker = 0;
				int iValue = 0;
				long lAmount;
				int iSharesColumn = -1;
				long lSharesValue = 0;
				int iPriceColumn = -1;
				double dPriceValue = 0;
				for (int i=0;i<arColumns.length;i++) {
					if (arColumns[i].equals(params.getDate()))
						iDate = i;
					if (arColumns[i].equals(params.getReference()))
						iRef = i;
					if (arColumns[i].equals(params.getDesc()))
						iDesc = i;
					if (arColumns[i].equals(params.getTicker()))
						iTicker = i;
					if (arColumns[i].equals(params.getValue()))
						iValue = i;
					if (arColumns[i].equals(params.getShares()))
						iSharesColumn = i;
					if (arColumns[i].equals(params.getPrice()))
						iPriceColumn = i;
				}
				while ((strLine = brPrices.readLine())!= null) {
					arColumns = splitString(strLine);
					for(int i=0; i<arColumns.length; i++) {
						arColumns[i] = arColumns[i].trim();
					}
					//System.err.println("old: " + arColumns.length);
					//arColumns = strLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
					//System.err.println("new: " + arColumns.length);

					//for(String t : arColumns) {
					//	System.out.println("> "+t);
					//}
					/*
					 * Amount is in pence, change to GBP
					 * 
					 * First check to see if amount had commas
					 */
					if (arColumns[iValue].startsWith("\"")) {
						String strAmount = arColumns[iValue].substring(1,arColumns[iValue].length());
						if (arColumns[iValue+1].endsWith("\"")) {
							strAmount += arColumns[iValue+1].substring(0,arColumns[iValue+1].length()-1);
						}
						else {
							strAmount += arColumns[iValue+1];
							strAmount += arColumns[iValue+2].substring(0,arColumns[iValue+2].length()-1);
						}
						arColumns[iValue] = strAmount;
					}
							
					JDateField jdtSettle = new JDateField (Main.cdate); 
					jdtSettle.setDate(jdtSettle.getDateFromString(arColumns[iDate]));
					// This is where we'd handle (dollars.cents) for negative values
					// Remove the dollar sign
					arColumns[iValue] = arColumns[iValue].replace("$", "");
					int iPoint = arColumns[iValue].indexOf('.');
					String strTemp = arColumns[iValue] + "00";
					if ( iPoint != -1) {
						strTemp = arColumns[iValue].substring(0,iPoint);
						String strDecimal = arColumns[iValue].substring(iPoint+1);
						switch (strDecimal.length()) {
						case 1:
							strTemp = strTemp+strDecimal+ "0";
							break;
						case 2:
							strTemp = strTemp+strDecimal;
							break;
						default :
							strTemp = strTemp+strDecimal.substring(0, 2);
						}
					}
					strTemp = strTemp.trim();
					if (strTemp.startsWith("(")) {
						strTemp = strTemp.substring(1, strTemp.length() - 1);
						if(strTemp.endsWith((")"))) {
							strTemp = strTemp.substring(0, strTemp.length() - 1);
						}
						strTemp = "-" + strTemp;
					}
					lAmount = Long.parseLong(strTemp);

					lSharesValue = 0L;
					dPriceValue = 0.0;
					if (iSharesColumn > 0 && iPriceColumn > 0) {
						try {
							lSharesValue = (long)(Double.parseDouble(arColumns[iSharesColumn])*1000);
						} catch (Exception ignore) {
							System.err.println("Could not parse as Long: " + arColumns[iSharesColumn]);
						}
						try {
							arColumns[iPriceColumn] = arColumns[iPriceColumn].replace("$", "");
							dPriceValue = Double.parseDouble(arColumns[iPriceColumn]);
						} catch (Exception ignore) {
							System.err.println("Could not parse as Double: " + arColumns[iPriceColumn]);
						}
						System.err.println("lSharesValue: " + lSharesValue + " dPriceValue: " + dPriceValue);
					} else {
						System.err.println("iSharesColumn: " + iSharesColumn + " iPriceColumn: " + iPriceColumn);
					}

					if (params.getExch()) {
						strTicker = arColumns[iTicker];
						int iPeriod = strTicker.indexOf('.');
						if (iPeriod > -1) {
							arColumns[iTicker] = strTicker.substring(0,iPeriod);
							strExchange = strTicker.substring(iPeriod+1);
						}
						else {
							iPeriod = strTicker.indexOf(':');
							if (iPeriod > -1) {
								arColumns[iTicker] = strTicker.substring(0,iPeriod);
								strExchange = strTicker.substring(iPeriod+1);								
							}
						}
							
					}
					else {
						strTicker = arColumns[iTicker];
						int iPeriod = strTicker.indexOf('.');
						if (iPeriod > -1) {
							strExchange = strTicker.substring(iPeriod+1);
						}
						else {
							iPeriod = strTicker.indexOf(':');
							if (iPeriod > -1) {
								strExchange = strTicker.substring(iPeriod+1);								
							}
						}						
					}
					SecLine objLine = new SecLine(jdtSettle.getDateInt(),arColumns[iRef],
							arColumns[iDesc],arColumns[iTicker]," ",lAmount,Main.mapAccounts.get(arColumns[iTicker]), lSharesValue, dPriceValue);
					if (params.isDefined(arColumns[iRef]))
						objLine.setIgnore(false);
					else
						objLine.setIgnore(true);
					if (params.requiresTicker(arColumns[iRef]) &&
							objLine.getTicker().equals(Constants.NOTICKER))
						objLine.setValid(false);
					Main.generatedTranSet.findTransaction(objLine);
					setLine.add(objLine);
				}
				brPrices.close();
			}
			catch (FileNotFoundException e) {
				JFrame fTemp = new JFrame();
				JOptionPane.showMessageDialog(fTemp,"File "+txtFileName+" not Found");
				close();
			}
			catch (IOException e) {
				JFrame fTemp = new JFrame();
				JOptionPane.showMessageDialog(fTemp,"I//O Error whilst reading "+txtFileName);
				close();
				
			}
			
	 }
	 
	 public void close() {
		this.setVisible(false);
		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
		topFrame.dispose();

	 }
	 private void generate() {
	      //Create and set up the window.
	      JFrame frame = new JFrame("Proposed Transactions - Build "+Main.buildStr);
	      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	      generateWindow = new GenerateWindow(setLine,acct,params);
	      frame.getContentPane().add(generateWindow);

	      //Display the window.
	      frame.pack();
	      frame.setLocationRelativeTo(null);
	      frame.setVisible(true);
	 }
	  /*
	   * Utility method to split a string containing both " and ,
	   */
	  
	  private String[] splitString(String strInput) {
		  List<String> listParts = new ArrayList<String>();
		  int i=0;
		  String strPart = "";
		  boolean bString = false;
		  while(i<strInput.length()) {
			switch (strInput.substring(i, i+1)) {
			case "\"" : 
				if (bString) {
					bString = false;
				}
				else
					bString = true;
				break;
			case "," :
				if (!bString) {
					listParts.add(strPart);
					strPart = "";
				}
				break;
			default :
				strPart += strInput.substring(i, i+1);
			}
			i++;
		  }
		  listParts.add(strPart);
		  String[] arrString = new String[listParts.size()];
		  return listParts.toArray(arrString);
	  }
	@Override
	public void tableChanged () {
		pricesModel.fireTableDataChanged();
		panMid.revalidate();
	}
}
