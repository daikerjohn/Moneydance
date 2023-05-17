/*
 *   Copyright (c) 2018, Michael Bray.  All rights reserved.
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
 * 
 */
package com.moneydance.modules.features.securityquoteload;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.infinitekind.moneydance.model.AccountListener;
import com.infinitekind.moneydance.model.CurrencyListener;
import com.infinitekind.moneydance.model.CurrencySnapshot;
import com.infinitekind.moneydance.model.CurrencyTable;
import com.infinitekind.moneydance.model.CurrencyType;
import com.infinitekind.moneydance.model.CurrencyUtil;
import com.moneydance.apps.md.controller.Util;
import com.moneydance.apps.md.view.MoneydanceUI;
import com.moneydance.apps.md.view.gui.MoneydanceGUI;
import com.moneydance.awt.GridC;
import com.moneydance.modules.features.mrbutil.MRBDebug;
import com.moneydance.modules.features.securityquoteload.Constants.QuoteSource;
import com.moneydance.modules.features.securityquoteload.view.CurTable;
import com.moneydance.modules.features.securityquoteload.view.CurTableModel;
import com.moneydance.modules.features.securityquoteload.view.CurrencyTab;
import com.moneydance.modules.features.securityquoteload.view.CurrencyTableLine;
import com.moneydance.modules.features.securityquoteload.view.ParameterTab;
import com.moneydance.modules.features.securityquoteload.view.SecTable;
import com.moneydance.modules.features.securityquoteload.view.SecTableModel;
import com.moneydance.modules.features.securityquoteload.view.SecurityCurrencyTab;
import com.moneydance.modules.features.securityquoteload.view.SecurityTab;
import com.moneydance.modules.features.securityquoteload.view.SecurityTableLine;

public class MainPriceWindow extends JFrame implements TaskListener, AccountListener, CurrencyListener {
	/**
	 * 
	 */
	protected SortedMap<String, SecurityTableLine> securitiesTable;
	protected SortedMap<String, CurrencyTableLine> currenciesTable;
	protected SortedMap<String, PseudoCurrency> pseudoCurrencies;
	protected SortedMap<String, String> selectedExchanges;
	protected SortedMap<String, String> alteredTickers;
	protected SortedMap<String, ExtraFields> volumes;
	protected SortedMap<QuoteSource, List<SecurityPrice>> sourceList;
	protected SortedMap<String, NewAccountLine> accountSources;
	protected JProgressBar tasksProgress;
	protected GetQuotesProgressMonitor listener;
	protected SecTableModel secPricesModel;
	protected SecTable secPricesDisplayTab;
	protected CurTableModel curRatesModel;
	protected CurTable curPricesDisplayTab;
	protected CurrencyType baseCurrency;
	protected String baseCurrencyID;
	protected Boolean completed = false;
	protected Boolean processCurrency = false;
	protected Boolean processSecurity = false;
	protected Boolean addVolume = false;
	protected int runtype = 0;
	protected double multiplier;
	protected boolean isSecDirty = false;
	protected boolean isCurDirty = false;

	/*
	 * Panels, Preferences and window sizes
	 */
	private JTabbedPane tabs;
	private JPanel mainScreen;
	private int iFRAMEWIDTH = Constants.FRAMEWIDTH;
	private int iFRAMEDEPTH = Constants.FRAMEHEIGHT;
	private JPanel buttonsPanel;
	private JPanel middlePanel;
	private JButton closeBtn;
	private JButton saveBtn;
	private JButton saveValBtn;
	private JButton getPricesBtn;
	private JButton getRatesBtn;
	private JButton exportBtn;
	private JButton selectAll;
	private JButton helpBtn;
	private Charset charSet = Charset.forName("UTF-8");
	private MoneydanceUI mdGUI;
	private com.moneydance.apps.md.controller.Main mdMain;
	/*
	 * Shared
	 */
	protected Parameters params;
	protected Main main;
	protected int closeBtnx = 0;
	protected int closeBtny = 0;
	protected String testTicker = "";
	protected String testTID = "";
	protected String command;
	protected boolean errorsFound = false;;
	protected List<String> errorTickers;
	protected SecurityTab securityScreen = null;
	protected SecurityCurrencyTab securityCurrencyScreen = null;
	protected CurrencyTab currencyScreen = null;
	protected ParameterTab parameterScreen = null;
	private String selectedTab;
	private boolean selectAllReturned = true;

	public MainPriceWindow(Main main, int runtype) {
		this.runtype = runtype;
		if (runtype != Constants.MANUALRUN && runtype != 0)
			return;
		mdMain = com.moneydance.apps.md.controller.Main.mainObj;
		mdGUI = mdMain.getUI();

		this.main = main;
		params = Parameters.getParameters();
		errorTickers = null;
		AccountBook book = Main.context.getCurrentAccountBook();
		book.addAccountListener(this);
		book.getCurrencies().addCurrencyListener(this);
		/*
		 * start of screen, set up tabs
		 */
		tabs = new JTabbedPane();
		securityScreen = new SecurityTab(params, main, this);
		securityCurrencyScreen = new SecurityCurrencyTab(params, main, this);
		currencyScreen = new CurrencyTab(params, main, this);
		parameterScreen = new ParameterTab(params, main, this);
		if (params.getCurrency()) {
			if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
				tabs.add(Constants.SECURITYTITLE, securityScreen);
				tabs.add(Constants.CURRENCYTITLE, currencyScreen);
			} else
				tabs.add(Constants.JOINTTITLE, securityCurrencyScreen);
		} else
			tabs.add(Constants.SECURITYTITLE, securityScreen);
		tabs.add(Constants.PARAMETERTITLE, parameterScreen);
		tabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JTabbedPane tabbedPaneT = (JTabbedPane) e.getSource();
				int selectedIndex = tabbedPaneT.getSelectedIndex();
				setButtons(selectedIndex);
				setPreferences();
				Main.debugInst.debug("MainPriceWindow", "tabChanged", MRBDebug.DETAILED,
						"New size for  " + selectedTab + " to " + iFRAMEWIDTH + "/" + iFRAMEDEPTH);
				Dimension newSize = new Dimension(iFRAMEWIDTH, iFRAMEDEPTH);
				getContentPane().setPreferredSize(newSize);
				pack();

			}
		});
		tabs.setSelectedIndex(0);
		mainScreen = new JPanel();
		this.add(mainScreen);

		/*
		 * set up listener for resizing
		 */
		mainScreen.setLayout(new BorderLayout());
		mainScreen.addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent arg0) {
				JPanel panScreen = (JPanel) arg0.getSource();
				Dimension objDimension = panScreen.getSize();
				updatePreferences(objDimension);
			}

			@Override
			public void componentShown(ComponentEvent arg0) {
				// not needed
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// not needed
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// not needed
			}

		});
		this.getRootPane().getActionMap().put("close-window", new CloseAction(this));
		this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke("control W"), "close-window");
		this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke("meta W"), "close-window");
		/*
		 * set up internal tables
		 */
		securitiesTable = new TreeMap<>();
		currenciesTable = new TreeMap<>();
		pseudoCurrencies = params.getPseudoCurrencies();
		/*
		 * Load base accounts and currencies
		 */
		accountSources = params.getSavedAccounts();
		loadAccounts(Main.context.getRootAccount());
		baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
		baseCurrencyID = baseCurrency.getIDString();
		loadCurrencies(Main.context.getCurrentAccountBook());
		/*
		 * set up screen tables
		 */
		int gridX = 0;
		int gridY = 0;
		secPricesModel = new SecTableModel(params, securitiesTable, this);
		secPricesDisplayTab = new SecTable(params, secPricesModel);
		curRatesModel = new CurTableModel(params, currenciesTable, this);
		curPricesDisplayTab = new CurTable(params, curRatesModel);
		if (params.getCurrency()) {
			if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
				securityScreen.setSecurityTable(secPricesDisplayTab);
				currencyScreen.setCurrencyTable(curPricesDisplayTab);
			} else
				securityCurrencyScreen.setTables(secPricesDisplayTab, curPricesDisplayTab);
		}
		else {
			securityScreen.setSecurityTable(secPricesDisplayTab);			
		}

		/*
		 * Prices Screen
		 */
		middlePanel = new JPanel(new BorderLayout());
		middlePanel.add(tabs, BorderLayout.CENTER);
		mainScreen.add(middlePanel, BorderLayout.CENTER);
		/*
		 * Buttons screen
		 */
		buttonsPanel = new JPanel(new GridBagLayout());
		gridX = 0;
		gridY = 0;
		if (selectAllReturned)
			selectAll = new JButton(Constants.SELECTALL);
		selectAll.setToolTipText("Selects/deselects all lines on the screen with prices");
		selectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (tabs.getSelectedIndex()) {
				case 0:
					if (selectAllReturned) {
						secPricesModel.selectAll(true);
						secPricesModel.fireTableDataChanged();
						if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME) {
							curRatesModel.selectAll(true);
							curRatesModel.fireTableDataChanged();
						}
						selectAllReturned = false;
						selectAll.setText(Constants.DESELECTALL);
					} else {
						secPricesModel.selectAll(false);
						secPricesModel.fireTableDataChanged();
						if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME) {
							curRatesModel.selectAll(false);
							curRatesModel.fireTableDataChanged();
						}
						selectAllReturned = true;
						selectAll.setText(Constants.SELECTALL);
					}
					break;
				case 1:
					if (selectAllReturned) {
						if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
							curRatesModel.selectAll(true);
							curRatesModel.fireTableDataChanged();
						}
						selectAllReturned = false;
						selectAll.setText(Constants.DESELECTALL);
					} else {
						if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
							curRatesModel.selectAll(false);
							curRatesModel.fireTableDataChanged();
						}
						selectAllReturned = true;
						selectAll.setText(Constants.SELECTALL);
					}
				}
			}
		});
		buttonsPanel.add(selectAll, GridC.getc(gridX++, gridY).insets(10, 10, 10, 10));
		/*
		 * Button Save Selected Values
		 */
		saveValBtn = new JButton("Save Selected Lines");
		saveValBtn.setToolTipText("Save the selected prices");
		saveValBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		buttonsPanel.add(saveValBtn, GridC.getc(gridX++, gridY).insets(10, 10, 10, 10));

		/*
		 * Get Rates
		 */
		getRatesBtn = new JButton("Get Exchange Rates");
		buttonsPanel.add(getRatesBtn, GridC.getc(gridX++, gridY).west().insets(10, 10, 10, 10));
		getRatesBtn.setToolTipText("Retrieve the exchange rates from the Internet");
		getRatesBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				processCurrency = false;
				processSecurity = false;
				switch (tabs.getSelectedIndex()) {
				case 0:
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME)
						processCurrency = true;
					break;
				case 1:
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE)
						processCurrency = true;
				}
				if (processCurrency)
					getPrices();
			}
		});
		/*
		 * Get
		 */
		getPricesBtn = new JButton("Get Prices");
		buttonsPanel.add(getPricesBtn, GridC.getc(gridX++, gridY).west().insets(10, 10, 10, 10));
		getPricesBtn.setToolTipText("Retrieve the quotes from the Internet");
		getPricesBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				processCurrency = false;
				processSecurity = false;
				switch (tabs.getSelectedIndex()) {
				case 0:
					processSecurity = true;
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME)
						processCurrency = true;
					break;
				case 1:
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE)
						processCurrency = true;
				}
				if (processCurrency || processSecurity)
					getPrices();
			}
		});
		/*
		 * Save
		 */
		saveBtn = new JButton("Save Changes");
		buttonsPanel.add(saveBtn, GridC.getc(gridX++, gridY).insets(10, 10, 10, 10));
		saveBtn.setToolTipText("Save the changes to the sources/ticker information");
		saveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Constants.SaveAction action = null;
				switch (tabs.getSelectedIndex()) {
				case 0:
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) 
						action = Constants.SaveAction.SECURITIES;
					else if (params.getCurrency())
						action = Constants.SaveAction.BOTH;
					else
						action = Constants.SaveAction.SECURITIES;
					break;
				case 1:
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE
							&& params.getCurrency())
						action = Constants.SaveAction.CURRENCIES;
				}
				if (action != null) {
					saveSourceChanges(action);
					switch (action) {
					case SECURITIES:
						isSecDirty=false;
						break;
					case CURRENCIES:
						isCurDirty=false;
						break;
					case BOTH:
						isSecDirty=false;
						isCurDirty=false;
						break;
					}
					JFrame fTemp = new JFrame();
					JOptionPane.showMessageDialog(fTemp, "Source/ticker information saved");
				}
			}
		});
		/*
		 * Export
		 */
		exportBtn = new JButton("Create Prices CSV");
		exportBtn.setToolTipText("Output selected prices to a .csv file");
		exportBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				processCurrency = false;
				processSecurity = false;
				switch (tabs.getSelectedIndex()) {
				case 0:
					processSecurity = true;
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME)
						processCurrency = true;
					break;
				case 1:
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE)
						processCurrency = true;
				}
				if (processCurrency || processSecurity)
					export();
			}
		});
		buttonsPanel.add(exportBtn, GridC.getc(gridX++, gridY).west().insets(10, 10, 10, 10));
		/*
		 * Help button
		 */
		helpBtn = new JButton("Help");
		helpBtn.setToolTipText("Display help information");
		helpBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String url = "https://bitbucket.org/mikerb/moneydance-2019/wiki/New%20Quote%20Loader";
				mdGUI.showInternetURL(url);
			}
		});
		buttonsPanel.add(helpBtn, GridC.getc(gridX++, gridY).west().insets(10, 10, 10, 10));

		/*
		 * Button Close
		 */
		closeBtn = new JButton("Close");
		closeBtn.setToolTipText("Close Quote Loader");
		closeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		buttonsPanel.add(closeBtn, GridC.getc(gridX++, gridY).west().insets(10, 10, 10, 10));
		closeBtnx = gridX;
		closeBtny = gridY;
		mainScreen.add(buttonsPanel, BorderLayout.PAGE_END);
		setButtons(0);
		setPreferences();
		getContentPane().setPreferredSize(new Dimension(iFRAMEWIDTH, iFRAMEDEPTH));
		this.pack();
	}

	private void setButtons(int selectedIndex) {
		switch (selectedIndex) {
		case 0:
			if (params.getCurrency()) {
				if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
					selectedTab = Constants.SELECTEDSECURITY;
					getRatesBtn.setVisible(false);
					getRatesBtn.setEnabled(false);
				} else {
					selectedTab = Constants.SELECTEDSECCUR;
					getRatesBtn.setVisible(true);
					getRatesBtn.setEnabled(true);
				}
			} else {
				selectedTab = Constants.SELECTEDSECURITY;
				getRatesBtn.setVisible(false);
				getRatesBtn.setEnabled(false);
			}
			selectAll.setVisible(true);
			saveValBtn.setVisible(true);
			getPricesBtn.setVisible(true);
			saveBtn.setVisible(true);
			exportBtn.setVisible(true);
			selectAll.setEnabled(true);
			saveValBtn.setEnabled(true);
			getPricesBtn.setEnabled(true);
			saveBtn.setEnabled(true);
			exportBtn.setEnabled(true);
			break;
		case 1:
			if (params.getCurrency() && params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
				selectedTab = Constants.SELECTEDCURRENCY;
				selectAll.setVisible(true);
				saveValBtn.setVisible(true);
				getRatesBtn.setVisible(true);
				getPricesBtn.setVisible(false);
				saveBtn.setVisible(true);
				exportBtn.setVisible(true);
				selectAll.setEnabled(true);
				saveValBtn.setEnabled(true);
				getRatesBtn.setEnabled(true);
				getPricesBtn.setEnabled(false);
				saveBtn.setEnabled(true);
				exportBtn.setEnabled(true);
			} else {
				selectedTab = Constants.SELECTEDPARAMETER;
				selectAll.setVisible(false);
				saveValBtn.setVisible(false);
				getRatesBtn.setVisible(false);
				getPricesBtn.setVisible(false);
				saveBtn.setVisible(false);
				exportBtn.setVisible(false);
				selectAll.setEnabled(false);
				saveValBtn.setEnabled(false);
				getRatesBtn.setEnabled(false);
				getPricesBtn.setEnabled(false);
				saveBtn.setEnabled(false);
				exportBtn.setEnabled(false);
			}
			break;
		case 2:
			selectedTab = Constants.SELECTEDPARAMETER;
			selectAll.setVisible(false);
			saveValBtn.setVisible(false);
			getRatesBtn.setVisible(false);
			getPricesBtn.setVisible(false);
			saveBtn.setVisible(false);
			exportBtn.setVisible(false);
			selectAll.setEnabled(false);
			saveValBtn.setEnabled(false);
			getRatesBtn.setEnabled(false);
			getPricesBtn.setEnabled(false);
			saveBtn.setEnabled(false);
			exportBtn.setEnabled(false);
		}
	}

	private void saveSourceChanges(Constants.SaveAction action) {
		if (action == Constants.SaveAction.SECURITIES || action == Constants.SaveAction.BOTH) {
			for (SecurityTableLine security : securitiesTable.values()) {
				if (accountSources.containsKey(security.getTicker())) {
					if (security.getSource() > 0 ) { 
						NewAccountLine line = accountSources.get(security.getTicker());
						line.setSource(security.getSource());
						line.setExchange(security.getExchange());
						line.setFtAlternate(security.getFtAlternate());
						line.setYahooAlternate(security.getYahooAlternate());
					} else
						accountSources.remove(security.getTicker());
				} else {
					if (security.getSource() > 0) { 
						NewAccountLine newLine = new NewAccountLine();
						newLine.setName(security.getTicker());
						newLine.setSource(security.getSource());
						newLine.setExchange(security.getExchange());
						newLine.setFtAlternate(security.getFtAlternate());
						newLine.setYahooAlternate(security.getYahooAlternate());
						accountSources.put(security.getTicker(), newLine);
					}
				}
			}
		}
		if (action == Constants.SaveAction.CURRENCIES || action == Constants.SaveAction.BOTH) {
			for (CurrencyTableLine currency : currenciesTable.values()) {
				if (accountSources.containsKey(currency.getTicker())) {
					if (currency.getSource() > 0) {
						NewAccountLine line = accountSources.get(currency.getTicker());
						line.setSource(currency.getSource());
						line.setExchange("");
					} else
						accountSources.remove(currency.getTicker());
				} else {
					if (currency.getSource() > 0) {
						NewAccountLine newLine = new NewAccountLine();
						newLine.setName(currency.getTicker());
						newLine.setSource(currency.getSource());
						newLine.setExchange("");
						newLine.setCurrency(true);
						accountSources.put(currency.getTicker(), newLine);
					}
				}
			}
		}
		params.saveAccountSources(accountSources);
	}

	public void processAction(Constants.MAINACTIONS action, String[] parameters) {
		Main.debugInst.debug("MainPriceWindow", "processAction", MRBDebug.DETAILED,
				"Callback action " + action.toString());
		switch (action) {
		case CHANGECURDISPLAY:
			String option = parameters[0];

			if (securityCurrencyScreen != null && securityCurrencyScreen.getParent() == tabs)
				tabs.remove(securityCurrencyScreen);
			if (securityScreen != null && securityScreen.getParent() == tabs)
				tabs.remove(securityScreen);
			if (currencyScreen != null && currencyScreen.getParent() == tabs)
				tabs.remove(currencyScreen);
			if (parameterScreen != null && parameterScreen.getParent() == tabs)
				tabs.remove(parameterScreen);
			if (params.getCurrency()) {
				if (option.equals("1")) {
					tabs.add(Constants.SECURITYTITLE, securityScreen);
					securityScreen.setSecurityTable(secPricesDisplayTab);
					tabs.add(Constants.CURRENCYTITLE, currencyScreen);
					currencyScreen.setCurrencyTable(curPricesDisplayTab);
				} else {
					tabs.add(Constants.JOINTTITLE, securityCurrencyScreen);
					securityCurrencyScreen.setTables(secPricesDisplayTab, curPricesDisplayTab);
				}
			} else {
				tabs.add(Constants.SECURITYTITLE, securityScreen);
				securityScreen.setSecurityTable(secPricesDisplayTab);
			}
			tabs.add(Constants.PARAMETERTITLE, parameterScreen);
			break;
		case GETCURRENCYRATES:
			break;
		case RESETDISPLAY:
			if (secPricesModel != null) {
				secPricesModel.resetNumberFormat();
				secPricesModel.fireTableDataChanged();
			}
			if (curRatesModel != null) {
				curRatesModel.resetNumberFormat();
				curRatesModel.fireTableDataChanged();
			}
			break;
		case CHANGEZERO:
			securitiesTable.clear();
			currenciesTable.clear();
			loadAccounts(Main.context.getRootAccount());
			baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
			baseCurrencyID = baseCurrency.getIDString();
			loadCurrencies(Main.context.getCurrentAccountBook());
			if (secPricesModel != null) {
				secPricesModel.resetData(securitiesTable);
				secPricesModel.fireTableDataChanged();
			}
			if (curRatesModel != null) {
				curRatesModel.resetData(currenciesTable);
				curRatesModel.fireTableDataChanged();
			}
			break;
		default:
			break;

		}
	}

	protected void resetData() {
		securitiesTable = new TreeMap<>();
		loadAccounts(Main.context.getRootAccount());
		if (params.getCurrency() || params.getZero()) {
			loadCurrencies(Main.context.getCurrentAccountBook());
			baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
			baseCurrencyID = baseCurrency.getIDString();
		}
		/*
		 * Clean up (removes any Account no longer in the list
		 */
		secPricesModel.resetData(securitiesTable);
		secPricesModel.fireTableDataChanged();
	}

	public void setErrorTickers(List<String> errorTickersp) {
		secPricesModel.addErrorTickers(errorTickersp);
		curRatesModel.addErrorTickers(errorTickersp);
	}

	/*
	 * Create single table containing all fields for a security
	 */
	protected void loadAccounts(Account parentAcct) {
		List<Account> acctList = parentAcct.getSubAccounts();
		for (Account acct : acctList) {
			if (acct.getAccountType() == Account.AccountType.SECURITY) {
				if ((acct.getBalance() != 0L) || (params.getZero())) {
					CurrencyType tickerCur = acct.getCurrencyType();
					/*
					 * Get last price entry
					 */
					if (tickerCur != null) {
						if (!tickerCur.getTickerSymbol().equals("")) {
							List<CurrencySnapshot> listSnap = tickerCur.getSnapshots();
							String ticker = tickerCur.getTickerSymbol().trim().toUpperCase();
							int iSnapIndex = listSnap.size() - 1;
							if (!securitiesTable.containsKey(ticker)) {
								NewAccountLine line = accountSources.get(ticker);
								SecurityTableLine dacct = new SecurityTableLine();
								dacct.setTicker(ticker);
								dacct.setAccountName(acct.getAccountName());
								dacct.setAccount(acct);
								dacct.setCurrencyType(tickerCur);
								dacct.setRelativeCurrencyType(tickerCur.getRelativeCurrency());
								if (!dacct.getRelativeCurrencyType().getIDString()
										.equals(baseCurrencyID))
									dacct.setDifferentCur(true);
								if (line != null) {
									dacct.setExchange(line.getExchange());
									dacct.setFtAlternate(line.getFtAlternate());
									dacct.setYahooAlternate(line.getYahooAlternate());
									dacct.setSource(line.getSource());
								} else
									dacct.setSource(0);
								if (iSnapIndex < 0) {
									dacct.setLastPrice(1.0);
									dacct.setPriceDate(0);
									securitiesTable.put(ticker, dacct);
								} else {
									CurrencySnapshot ctssLast = listSnap.get(iSnapIndex);
									if (ctssLast != null) {
										dacct.setLastPrice(1.0 / ctssLast.getRate());
									} else {
										dacct.setLastPrice(0.0);
									}
									dacct.setPriceDate(ctssLast.getDateInt());
									securitiesTable.put(ticker, dacct);
								}
							}
						}
					}
				}
			}
			loadAccounts(acct);
		}
	}

	/*
	 * Load the currencies and add to Securities
	 */
	protected void loadCurrencies(AccountBook accountBook) {
		CurrencyTable currencyTable = accountBook.getCurrencies();
		Iterator<CurrencyType> currTypeIterator = currencyTable.iterator();
		int snapIndex;
		List<CurrencySnapshot> listSnap;
		while (currTypeIterator.hasNext()) {
			CurrencyType currencyType = currTypeIterator.next();
			if (currencyType.getCurrencyType() == CurrencyType.Type.SECURITY && params.getZero()) {
				if (!currencyType.getTickerSymbol().equals("") && !currencyType.getHideInUI()) {
					listSnap = currencyType.getSnapshots();
					String ticker = currencyType.getTickerSymbol().trim().toUpperCase();
					snapIndex = listSnap.size() - 1;
					if (!securitiesTable.containsKey(ticker)) {
						NewAccountLine sourceLine = accountSources.get(ticker);
						SecurityTableLine securityLine = new SecurityTableLine();
						securityLine.setAccount(null);
						securityLine.setTicker(ticker);
						securityLine.setCurrencyType(currencyType);
						securityLine.setAccountName(currencyType.getName());
						securityLine.setRelativeCurrencyType(currencyType.getRelativeCurrency());
						if (!securityLine.getRelativeCurrencyType().getIDString().equals(baseCurrencyID))
							securityLine.setDifferentCur(true);
						if (sourceLine != null) {
							securityLine.setExchange(sourceLine.getExchange());
							securityLine.setFtAlternate(sourceLine.getFtAlternate());
							securityLine.setYahooAlternate(sourceLine.getYahooAlternate());
							securityLine.setSource(sourceLine.getSource());
						} else {
							securityLine.setSource(0);
							securityLine.setExchange(null);
							securityLine.setFtAlternate(null);
							securityLine.setYahooAlternate(null);
						}
						if (snapIndex < 0) {
							securityLine.setLastPrice(1.0);
							securityLine.setPriceDate(0);
							securitiesTable.put(ticker, securityLine);
						} else {
							CurrencySnapshot ctssLast = listSnap.get(snapIndex);
							if (ctssLast != null) {
								securityLine.setLastPrice(1.0 / ctssLast.getRate());
							} else {
								securityLine.setLastPrice(0.0);
							}
							securityLine.setPriceDate(ctssLast.getDateInt());
							securitiesTable.put(ticker, securityLine);
						}
					}
				}
			} else {
				if (currencyType.getCurrencyType() == CurrencyType.Type.CURRENCY && !currencyType.getHideInUI()
						&& currencyType != baseCurrency) {
					listSnap = currencyType.getSnapshots();
					snapIndex = listSnap.size() - 1;
					CurrencyTableLine dummyCur = new CurrencyTableLine(
							Constants.CURRENCYID + currencyType.getIDString(), currencyType);
					NewAccountLine line = accountSources.get(dummyCur.getTicker());
					dummyCur.setCurrencyName(currencyType.getName());
					if (line != null)
						dummyCur.setSource(line.getSource());
					else
						dummyCur.setSource(0);
					if (snapIndex < 0) {
						dummyCur.setLastPrice(1.0);
						dummyCur.setPriceDate(0);
						currenciesTable.put(Constants.CURRENCYID + currencyType.getIDString(), dummyCur);
					} else {
						CurrencySnapshot ctssLast = listSnap.get(snapIndex);
						if (ctssLast == null)
							dummyCur.setLastPrice(1.0);
						else
							dummyCur.setLastPrice(ctssLast.getRate());
						if (dummyCur.getLastPrice().isInfinite())
							dummyCur.setLastPrice(1.0);
						dummyCur.setPriceDate(ctssLast.getDateInt());
						currenciesTable.put(Constants.CURRENCYID + currencyType.getIDString(), dummyCur);
					}
				}
			}
		}
	}

	public void setIsSecDirty(boolean isDirty) {
		this.isSecDirty = isDirty;
	}
	public void setIsCurDirty(boolean isDirty) {
		this.isCurDirty = isDirty;
	}
	public boolean isSecDirty() {
		return isSecDirty;
	}
	public boolean isCurDirty() {
		return isCurDirty;
	}

	public boolean isParamDirty() {
		return params.paramsChanged();
	}
	public void checkUnsaved() {
		if (isSecDirty && isCurDirty) {
			if (JOptionPane.showConfirmDialog(this,
					"You have changed both the Securities and Exchange Rates source/ticker information.  Do you wish to save it before closing?",
					"Save Source/Ticker", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				saveSourceChanges(Constants.SaveAction.BOTH);
			}
			isSecDirty=false;
			isCurDirty=false;
		}
		if (isSecDirty) {
			if (JOptionPane.showConfirmDialog(this,
					"You have changed the Securities source/ticker information.  Do you wish to save it before closing?",
					"Save Source/Ticker", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				saveSourceChanges(Constants.SaveAction.SECURITIES);
			}
			isSecDirty=false;
		}
		if (isCurDirty) {
			if (JOptionPane.showConfirmDialog(this,
					"You have changed the Exchange Rate source/ticker information.  Do you wish to save it before closing?",
					"Save Source/Ticker", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				saveSourceChanges(Constants.SaveAction.CURRENCIES);
			}
			isCurDirty=false;
		}
		if (params.paramsChanged()) {
			if (JOptionPane.showConfirmDialog(this,
					"You have changed the Parameters.  Do you wish to save them before closing?",
					"Save Parameters", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				params.save();
				JOptionPane.showMessageDialog(null, "Changes Saved");
			}
		}
		params.setDirty(false);
	}
	public void close() {
		checkUnsaved();
		this.setVisible(false);
		if (main != null)
			main.cleanup();

	}

	private void export() {
		String exportFolder = params.getExportFolder();
		if (exportFolder == null || exportFolder.isEmpty()) {
			JOptionPane.showMessageDialog(null, "Export folder has not been set");
			return;
		}
		int iRowCount = 0;
		boolean pricesFound = false;
		if (processSecurity) {
			for (SecurityTableLine line : securitiesTable.values()) {
				if (line.getNewPrice() != 0.0)
					pricesFound = true;
				if (line.getSelected())
					iRowCount++;
			}
		}
		if (processCurrency) {
			for (CurrencyTableLine line : currenciesTable.values()) {
				if (line.getNewPrice() != 0.0)
					pricesFound = true;
				if (line.getSelected())
					iRowCount++;
			}
		}
		if (!pricesFound) {
			JOptionPane.showMessageDialog(null,
					"No prices have been downloaded.  Use Get Exchange Rates or Get Prices");
			return;
		}
		if (iRowCount < 1) {
			JOptionPane.showMessageDialog(null,
					"No prices have been selected.  Select individual lines or Select All.");
			return;
		}
		BufferedWriter exportFile = setupExportFile();
		iRowCount = 0;
		switch (tabs.getSelectedIndex()) {
		case 0:
			for (SecurityTableLine line : securitiesTable.values()) {
				if (line.getSelected()) {
					secPricesModel.updateLine(line, exportFile, true);
					iRowCount++;
				}

			}
			if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME) {
				for (CurrencyTableLine line : currenciesTable.values()) {
					if (line.getSelected()) {
						curRatesModel.updateLine(line, exportFile, true);
						iRowCount++;
					}
				}
			}
			break;
		case 1:
			if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
				for (CurrencyTableLine line : currenciesTable.values()) {
					if (line.getSelected()) {
						curRatesModel.updateLine(line, exportFile, true);
						iRowCount++;
					}
				}
			}

		}
		if (exportFile != null)
			try {
				exportFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		JOptionPane.showMessageDialog(null, "Prices Exported.");

	}

	private BufferedWriter setupExportFile() {
		FileOutputStream exportFile;
		OutputStreamWriter exportWriter;
		BufferedWriter exportBuffer;
		String filename = params.getExportFolder() + "/";
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		filename += "priceexport" + dtf.format(LocalDateTime.now()) + ".csv";
		try {
			exportFile = new FileOutputStream(filename);
			exportWriter = new OutputStreamWriter(exportFile, "UTF-8");
			exportBuffer = new BufferedWriter(exportWriter);
			exportWriter.write(Constants.EXPORTHEADER);
			Main.debugInst.debug("MainPriceWindow", "setupExportFile", MRBDebug.DETAILED,
					"Created export file" + filename);
		} catch (IOException e) {
			exportBuffer = null;
		}
		return exportBuffer;
	}

	protected void save() {
		BufferedWriter exportFile;
		Main.isUpdating = true;
		int iRowCount = 0;
		boolean pricesFound=false;
		for (SecurityTableLine line : securitiesTable.values()) {
			if (line.getNewPrice()!=0.0)
				pricesFound=true;
			if (line.getSelected())
				iRowCount++;
		}
		for (CurrencyTableLine line : currenciesTable.values()) {
			if (line.getNewPrice()!=0.0)
				pricesFound=true;
			if (line.getSelected())
				iRowCount++;
		}
		if (runtype != Constants.MANUALRUN && runtype != 0) {
			if (params.isExportAuto()) {
				exportFile = setupExportFile();
			} else
				exportFile = null;
			if (processSecurity) {
				for (SecurityTableLine line : securitiesTable.values()) {
					if (line.getSelected())
						secPricesModel.updateLine(line, exportFile, false);
					else
						line.setTickerStatus(0);
				}
			}
			if (processCurrency) {
				for (CurrencyTableLine line : currenciesTable.values()) {
					if (line.getSelected())
						curRatesModel.updateLine(line, exportFile, false);
					else
						line.setTickerStatus(0);
				}
				curRatesModel.fireTableDataChanged();

			}
			if (exportFile != null)
				try {
					exportFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		} else {
			if (!pricesFound) {
				JOptionPane.showMessageDialog(null,
						"No prices have been downloaded.  Use Get Exchange Rates or Get Prices");
				return;
			}
			if (iRowCount < 1) {
				JOptionPane.showMessageDialog(null,
						"No prices have been selected.  Select individual lines or Select All.");
				return;
			}
			if (params.isExport()) {
				exportFile = setupExportFile();
			} else
				exportFile = null;
			switch (tabs.getSelectedIndex()) {
			case 0:
				secPricesModel.updateLines(exportFile, false);
				selectAll.setText(Constants.SELECTALL);
				selectAllReturned = true;
				secPricesModel.fireTableDataChanged();
				if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME) {
					curRatesModel.updateLines(exportFile, false);
					curRatesModel.fireTableDataChanged();

				}
				break;
			case 1:
				if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE) {
					curRatesModel.updateLines(exportFile, false);
					curRatesModel.fireTableDataChanged();

				}
				selectAllReturned = true;
				selectAll.setText(Constants.SELECTALL);
			}
			if (exportFile != null)
				try {
					exportFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		/*
		 * if an automatic run it is expected that this object will be disposed off
		 */
		if (runtype == Constants.MANUALRUN) {
			JOptionPane.showMessageDialog(null,iRowCount+" prices updated");
		}
		Main.isUpdating = false;
	}

	protected void getPrices() {
		completed = false;
		if (processSecurity) {
			for (SecurityTableLine line : securitiesTable.values())
				line.setTickerStatus(0);
			secPricesModel.resetPrices();
			secPricesModel.clearErrorTickers();
			secPricesModel.resetHistory();
		}
		if (processCurrency) {
			for (CurrencyTableLine line : currenciesTable.values())
				line.setTickerStatus(0);
			curRatesModel.resetPrices();
			curRatesModel.clearErrorTickers();
			curRatesModel.resetHistory();
		}
		alteredTickers = new TreeMap<>();
		if (runtype == Constants.MANUALRUN) {
			final JProgressBar taskProgress = new JProgressBar(0, 100);
			taskProgress.setValue(0);
			taskProgress.setStringPainted(true);
			buttonsPanel.add(taskProgress, GridC.getc(closeBtnx, closeBtny).insets(10, 10, 10, 10));
			this.tasksProgress = taskProgress;
			this.listener = new GetQuotesProgressMonitor(this.tasksProgress, this, securitiesTable,
					this.currenciesTable);
		} else
			this.listener = new GetQuotesProgressMonitor(null, this, securitiesTable, this.currenciesTable);
		if (runtype == Constants.MANUALRUN) {
			if (processSecurity)
				getPricesBtn.setEnabled(false);
			if (processCurrency)
				getRatesBtn.setEnabled(false);
		}
		sourceList = new TreeMap<QuoteSource, List<SecurityPrice>>();
		for (Integer srce : Constants.SOURCELIST) {
			QuoteSource.findSource(srce).setUuid(UUID.randomUUID().toString());
			sourceList.put(QuoteSource.findSource(srce), new ArrayList<SecurityPrice>());
		}
		if (processSecurity) {
			for (SecurityTableLine secLine : securitiesTable.values()) {
				if (secLine.getSource() == 0)
					continue;
				if (secLine.getTicker().indexOf(Constants.TICKEREXTID)>0)
					continue;
				QuoteSource qs = QuoteSource.findSource(secLine.getSource());
				SecurityPrice spLine = new SecurityPrice(secLine.getTicker());
				if (secLine.getExchange() != "")
					spLine.setExchange(secLine.getExchange());
				else
					spLine.setExchange(null);
				spLine.setAlternate(secLine.getAlternateTicker());
				sourceList.get(qs).add(spLine);
			}

		}
		if (processCurrency) {
			for (CurrencyTableLine curLine : currenciesTable.values()) {
				if (curLine.getSource() == 0)
					continue;
				QuoteSource qs = QuoteSource.findSource(curLine.getSource());
				SecurityPrice spLine = new SecurityPrice(curLine.getTicker());
				spLine.setExchange(null);
				sourceList.get(qs).add(spLine);
			}
		}
		int totalQuotes = 0;
		for (List<SecurityPrice> item : sourceList.values())
			totalQuotes += item.size();
		if (totalQuotes == 0) {
			if (runtype == Constants.MANUALRUN) {
				if (tabs.getSelectedIndex() == 0) {
					getPricesBtn.setEnabled(true);
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME)
						getRatesBtn.setEnabled(true);
				}
				if (tabs.getSelectedIndex() == 1) {
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE)
						getRatesBtn.setEnabled(true);
				}
			}

			else {
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Main.context.showURL("moneydance:fmodule:" + Constants.PROGRAMNAME + ":"
								+ Constants.AUTODONECMD);
					}
				});
			}

			return;
		}
		listener.setSubTaskSize(totalQuotes);
		completed = false;
		/*
		 * let main process know we are starting a quote
		 */
		Main.context.showURL("moneydance:fmodule:" + Constants.PROGRAMNAME + ":" + Constants.STARTQUOTECMD
				+ "?numquotes=" + totalQuotes);
		Integer lastPriceDate = -1;
		for (QuoteSource srce : QuoteSource.values()) {
			if (sourceList.get(srce) == null || sourceList.get(srce).isEmpty())
				continue;

			String url = null;
			String type = "";
			String ticker = "";
			for (SecurityPrice price : sourceList.get(srce)) {
				if (price.isCurrency()) {
					type = Constants.CURRENCYTYPE;
					String currency = price.getCurrency();
					CurrencyTableLine currLine = currenciesTable.get(price.getTicker());
					if (currency.endsWith("-"))
						ticker = currency + baseCurrencyID;
					else {
						switch (srce) {
						case FT:
						case FTHD:
							ticker = baseCurrencyID + currency;
							lastPriceDate = currLine.getPriceDate();
							break;
						case YAHOO:
						case YAHOOHD:
						case YAHOOTD:
							if (baseCurrencyID.equals("USD"))
								ticker = currency + Constants.CURRENCYTICKER;
							else
								ticker = baseCurrencyID + currency + Constants.CURRENCYTICKER;
							lastPriceDate = currLine.getPriceDate();
							break;
						default:
							break;
						}
					}
				} else {
					type = Constants.STOCKTYPE;
					ticker = price.getTicker();
					String newTicker = params.getNewTicker(ticker, price.getExchange(), price.getAlternate(),
							srce.getSource());
					SecurityTableLine line = securitiesTable.get(ticker);
					switch (srce) {
					case YAHOOHD:
					case FTHD:
						lastPriceDate = line.getPriceDate();
						break;
					case FT:
					case YAHOO:
					case YAHOOTD:
						lastPriceDate = -1;
						break;
					default:
						break;

					}
					if (!newTicker.equals(ticker)) {
						alteredTickers.put(newTicker, ticker);
						Main.debugInst.debug("MainPriceWindow", "getPrices", MRBDebug.DETAILED,
								"Ticker changed from " + ticker + " to " + newTicker);
						ticker = newTicker;
					}
				}
				if (url == null)
					url = newPriceUrl(Constants.SOURCES[srce.getSource() - 1], srce.getUuid(), ticker, type,
							lastPriceDate);
				else
					url = url + addPriceUrl(ticker, type, lastPriceDate);
				if (listener != null)
					listener.started(price.getTicker(), srce.getUuid());
			}
			Main.debugInst.debug("MainPriceWindow", "getPrices", MRBDebug.INFO, "URI " + url);
			Main.context.showURL(url);
		}

	}

	protected String newPriceUrl(String source, String tid, String stock, String type, Integer lastPriceDate) {
		String queries = addPriceUrl(stock, type, lastPriceDate);
		String command = Constants.GETQUOTECMD + "?" + Constants.SOURCETYPE + "=" + source;
		command += "&" + Constants.TIDCMD + "=" + tid;
		String url = "moneydance:fmodule:" + Main.extension.serverName + ":" + command + queries;
		return url;
	}

	protected String addPriceUrl(String stock, String type, Integer lastPriceDate) {
		List<NameValuePair> parameters = new ArrayList<>();
		parameters.add(new BasicNameValuePair(type, stock));
		if (lastPriceDate > -1 && params.getHistory()) {
			parameters.add(new BasicNameValuePair(Constants.LASTPRICEDATETYPE, lastPriceDate.toString()));
		}
		String charset = "UTF8";
		String queries = URLEncodedUtils.format(parameters, charset);
		return "&" + queries;
	}

	public void testTicker(String url) {
		Main.debugInst.debug("MainPriceWindow", "testTicker", MRBDebug.INFO, "URI " + url);
		URI uri = null;
		String convUrl = url.replace("^", "%5E");
		convUrl = convUrl.replace(" ", "%20");
		try {
			uri = new URI(convUrl.trim());
		} catch (URISyntaxException e) {
			Main.debugInst.debug("MainPriceWindow", "testTicker", MRBDebug.DETAILED, "URI invalid " + convUrl);
			e.printStackTrace();
			return;
		}
		List<NameValuePair> results = URLEncodedUtils.parse(uri, charSet);
		String ticker = "";
		String source = "";
		for (NameValuePair price : results) {
			if (price.getName().compareToIgnoreCase(Constants.STOCKTYPE) == 0) {
				ticker = price.getValue();
			}
			if (price.getName().compareToIgnoreCase(Constants.SOURCETYPE) == 0) {
				source = price.getValue();
			}
		}
		if (!ticker.isEmpty()) {
			testTID = UUID.randomUUID().toString();
			testTicker = ticker;
			String testurl = newPriceUrl(source, testTID, ticker, Constants.STOCKTYPE, 0);
			Main.debugInst.debug("MainPriceWindow", "testTicker", MRBDebug.DETAILED, "URI " + testurl);
			Main.context.showURL(testurl);
		}
	}

	public synchronized void updatePrices(String url) {
		String uuid = "";
		Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO, "URI " + url);
		URI uri = null;
		String convUrl = url.replace("^", "%5E");
		convUrl = convUrl.replace(" ", "%20");
		try {

			uri = new URI(convUrl.trim());
		} catch (URISyntaxException e) {
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED, "URI invalid " + convUrl);
			e.printStackTrace();
			return;
		}
		/*
		 * capture data from returned url
		 */
		List<NameValuePair> results = URLEncodedUtils.parse(uri, charSet);
		SecurityPrice newPrice = null;
		NumberFormat nf = NumberFormat.getNumberInstance();
		QuoteSource srce = null;
		for (NameValuePair price : results) {
			if (price.getName().compareToIgnoreCase(Constants.STOCKTYPE) == 0) {
				newPrice = new SecurityPrice(price.getValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.CURRENCYTYPE) == 0) {
				String ticker = price.getValue();
				if (ticker.endsWith(Constants.CURRENCYTICKER)) {
					switch (srce) {
					case FT:
					case FTHD:
						ticker = Constants.CURRENCYID
								+ ticker.substring(3, ticker.indexOf(Constants.CURRENCYTICKER));
						break;
					case YAHOO:
					case YAHOOHD:
					case YAHOOTD:
						if (baseCurrencyID.equals("USD"))
							ticker = Constants.CURRENCYID
									+ ticker.substring(0, ticker.indexOf(Constants.CURRENCYTICKER));
						else
							ticker = Constants.CURRENCYID
									+ ticker.substring(3, ticker.indexOf(Constants.CURRENCYTICKER));
						break;
					default:
						break;
					}
				} else {
					if (ticker.contains("-")) {
						ticker = Constants.CURRENCYID + ticker.substring(0, ticker.indexOf('-') + 1);
					} else {
						switch (srce) {
						case FT:
						case FTHD:
							ticker = Constants.CURRENCYID + ticker.substring(3);
							break;
						case YAHOO:
						case YAHOOHD:
						case YAHOOTD:
							if (baseCurrencyID.equals("USD") && !uuid.isEmpty())
								ticker = Constants.CURRENCYID + ticker.substring(0, 2);
							else
								ticker = Constants.CURRENCYID + ticker.substring(3);
							break;
						default:
							break;

						}
					}
				}

				newPrice = new SecurityPrice(ticker);
			}
			if (price.getName().compareToIgnoreCase(Constants.PRICETYPE) == 0) {
				if (newPrice != null) {
					String priceStr = price.getValue();
					ParsePosition pos = new ParsePosition(0);
					Number newNum = nf.parse(priceStr, pos);
					if (pos.getIndex() != priceStr.length()) {
						Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO,
								"Invalid price returned for " + newPrice.getTicker() + " " + priceStr);
						newPrice.setSecurityPrice(0.0);
					} else
						newPrice.setSecurityPrice(newNum.doubleValue());
				}
			}
			if (price.getName().compareToIgnoreCase(Constants.TRADEDATETYPE) == 0) {
				String isoDate = price.getValue();
				if (isoDate.equals(Constants.MISSINGDATE)) {
					ZonedDateTime today = ZonedDateTime.now(ZoneId.systemDefault());
					isoDate = today.toString();
				}
				String tradeDate = isoDate.substring(0, isoDate.indexOf("T"));
				tradeDate = tradeDate.replaceAll("-", "");
				Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED,
						"Trade Dates Sent=" + isoDate + " extracted=" + tradeDate);
				newPrice.setTradeDate(Integer.parseInt(tradeDate));
			}
			if (price.getName().compareToIgnoreCase(Constants.TRADECURRTYPE) == 0) {
				newPrice.setCurrency(price.getValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.TIDCMD) == 0) {
				uuid = price.getValue();
				for (QuoteSource srcet : QuoteSource.values()) {
					if (uuid.equals(srcet.getUuid())) {
						srce = srcet;
						break;
					}
				}
			}
			if (price.getName().compareToIgnoreCase(Constants.VOLUMETYPE) == 0) {
				newPrice.setVolume(Long.parseLong(price.getValue()));
			}
			if (price.getName().compareToIgnoreCase(Constants.HIGHTYPE) == 0) {
				String priceStr = price.getValue();
				ParsePosition pos = new ParsePosition(0);
				Number newNum = nf.parse(priceStr, pos);
				if (pos.getIndex() != priceStr.length()) {
					Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO,
							"Invalid high price returned for " + newPrice.getTicker() + " " + priceStr);
					newPrice.setHighPrice(0.0);
				} else
					newPrice.setHighPrice(newNum.doubleValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.LOWTYPE) == 0) {
				String priceStr = price.getValue();
				ParsePosition pos = new ParsePosition(0);
				Number newNum = nf.parse(priceStr, pos);
				if (pos.getIndex() != priceStr.length()) {
					Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO,
							"Invalid low price returned for " + newPrice.getTicker() + " " + priceStr);
					newPrice.setLowPrice(0.0);
				} else
					newPrice.setLowPrice(newNum.doubleValue());
			}
		}
		/*
		 * data extracted test for test ticker
		 */
		if (newPrice.getTicker().equals(testTicker) && testTID.equals(uuid)) {
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED, "Test Ticker returned");
			testTicker = "";
			testTID = "";
			String message = "Test of security " + newPrice.getTicker() + " was successful. Price "
					+ newPrice.getSecurityPrice() + " Currency " + newPrice.getCurrency();
			JOptionPane.showMessageDialog(null, message);
			return;
		}
		/*
		 * check for altered by exchange
		 */
		if (alteredTickers != null && alteredTickers.containsKey(newPrice.getTicker())) {
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED, "Ticker changed from "
					+ newPrice.getTicker() + " to " + alteredTickers.get(newPrice.getTicker()));
			newPrice.setTicker(alteredTickers.get(newPrice.getTicker()));
		}
		if (completed) {
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO, "Late message");
			return;
		}
		if (listener == null) {
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO,
					"Update received after close " + uuid);
			return;
		}
		if (!listener.checkTid(uuid)) {
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO,
					"Update received after close " + uuid);
			return;
		}
		if (newPrice.getSecurityPrice() == 0.0) {
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.INFO,
					"No price returned for " + newPrice.getTicker());
			return;
		}

		/*
		 * we have a price
		 */
		Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.SUMMARY,
				"Updating prices for " + newPrice.getTicker());
		String ticker = newPrice.getTicker();
		SecurityTableLine secLine = null;
		CurrencyTableLine curLine = null;
		if (newPrice.isCurrency())
			curLine = currenciesTable.get(ticker);
		else
			secLine = securitiesTable.get(ticker);

		if (runtype != Constants.MANUALRUN && runtype != 0) {
			if (newPrice.isCurrency())
				Main.context.showURL("moneydance:setprogress?meter=0&label=Quote Loader price "
						+ newPrice.getCurrency() + " updated");
			else
				Main.context.showURL(
						"moneydance:setprogress?meter=0&label=Quote Loader price " + ticker + " updated");
		}
		Double dRate = 1.0;
		CurrencyType securityCur = null;
		String tradeCur = newPrice.getCurrency();
		Double stockPrice = 0.0;
		Double lowPrice = 0.0;
		Double highPrice = 0.0;
		/*
		 * check to see if trade currency is in the pseudocurrency file
		 */
		if (newPrice != null && !newPrice.isCurrency() && pseudoCurrencies.containsKey(tradeCur)) {
			PseudoCurrency line = pseudoCurrencies.get(newPrice.getCurrency());
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.SUMMARY,
					"Pseudo Currency Detected " + newPrice.getCurrency() + " for " + newPrice.getTicker());
			Double oldValue = newPrice.getSecurityPrice();
			stockPrice = oldValue * line.getMultiplier();
			oldValue = newPrice.getLowPrice();
			lowPrice = oldValue * line.getMultiplier();
			oldValue = newPrice.getHighPrice();
			highPrice = oldValue * line.getMultiplier();
			tradeCur = line.getReplacement();
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.SUMMARY,
					"Price changed from " + oldValue + " to " + stockPrice);
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.SUMMARY,
					"Currency changed from " + line.getPseudo() + " to " + line.getReplacement());
		} else {
			stockPrice = newPrice.getSecurityPrice();
			lowPrice = newPrice.getLowPrice();
			highPrice = newPrice.getHighPrice();
		}
		/*
		 * get currency type for trade
		 */
		CurrencyType tradeCurType = Main.context.getCurrentAccountBook().getCurrencies()
				.getCurrencyByIDString(tradeCur);
		if (tradeCurType == null)
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED, "No Price currency ");
		else
			Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED,
					"Price currency " + tradeCurType.getIDString());
		/*
		 * check trade for stock/currency
		 */
		if (!newPrice.isCurrency()) {
			/*
			 * trade is stock
			 */
			securityCur = secLine.getRelativeCurrencyType();
			if (securityCur != null)
				Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED,
						"Relative currency " + securityCur.getIDString());
			else
				Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED,
						"No Relative currency ");
			int currencyDate;
			currencyDate = newPrice.getTradeDate();
			if (tradeCurType != null && securityCur != null) {
				if (!tradeCurType.equals(securityCur)) {
					/*
					 * trade currency is not security currency
					 */
					dRate = CurrencyUtil.getUserRate(tradeCurType, securityCur, currencyDate);
					Main.debugInst.debug("MainPriceWindow", "updatePrices", MRBDebug.DETAILED,
							"quote to security rate " + dRate);
				}
			}
		} else {
			stockPrice = newPrice.getSecurityPrice();
			lowPrice = newPrice.getLowPrice();
			highPrice = newPrice.getHighPrice();
		}
		/*
		 * Assume the price will be displayed in the currency of the security
		 */
		multiplier = Math.pow(10.0, Double.valueOf(params.getDecimal()));
		if (!newPrice.isCurrency()) {
			Main.debugInst.debug("MainPriceWindow", "updatePrice", MRBDebug.DETAILED,
					"before rounding" + stockPrice);
			stockPrice = Math.round(stockPrice * multiplier) / multiplier;
			lowPrice = Math.round(lowPrice * multiplier) / multiplier;
			highPrice = Math.round(highPrice * multiplier) / multiplier;
			Main.debugInst.debug("MainPriceWindow", "updatePrice", MRBDebug.DETAILED,
					"after rounding" + stockPrice);
		}
		double amtChg;
		double perChg;
		if (stockPrice != 0.0) {
			if (newPrice.isCrypto()) {
				curLine.setNewPrice(1 / Util.safeRate(stockPrice));
				curLine.setTradeDate(newPrice.getTradeDate());
				amtChg = Math.round((curLine.getNewPrice() - curLine.getLastPrice()) * multiplier)
						/ multiplier;
				curLine.setAmtChg(amtChg);
				perChg = Math.round(((amtChg / (curLine.getLastPrice()) * 100.0)) * multiplier)
						/ multiplier;
				curLine.setPercentChg(perChg);
				curRatesModel.fireTableDataChanged();
				curLine.clearHistory();
			} else {
				if (newPrice.isCurrency()) {
					curLine.setTradeDate(newPrice.getTradeDate());
					curLine.setNewPrice(Util.safeRate(stockPrice));
					amtChg = Math.round((curLine.getNewPrice() - curLine.getLastPrice()) * multiplier)
							/ multiplier;
					curLine.setAmtChg(amtChg);
					perChg = Math.round(((amtChg / (curLine.getLastPrice()) * 100.0)) * multiplier)
							/ multiplier;
					curLine.setPercentChg(perChg);
					curRatesModel.fireTableDataChanged();
					curLine.clearHistory();
				} else {
					secLine.setNewPrice(stockPrice * dRate);
					secLine.setTradeDate(newPrice.getTradeDate());
					secLine.setTradeCur(newPrice.getCurrency());
					secLine.setQuotedPrice(newPrice.getSecurityPrice());
					amtChg = Math.round((secLine.getNewPrice() - secLine.getLastPrice()) * multiplier)
							/ multiplier;
					secLine.setAmtChg(amtChg);
					perChg = Math.round(((amtChg / (secLine.getLastPrice()) * 100.0)) * multiplier)
							/ multiplier;
					secLine.setPercentChg(perChg);
					ExtraFields extra = new ExtraFields(newPrice.getVolume(), highPrice * dRate,
							lowPrice * dRate);
					secLine.setVolume(extra);
					secLine.clearHistory();
					secPricesModel.fireTableDataChanged();
				}
			}
		}
		if (listener != null)
			listener.ended(newPrice.getTicker(), uuid);
	}

	public synchronized void updateHistory(String url) {
		String uuid = "";
		URI uri = null;
		String convUrl = url.replace("^", "%5E");
		try {

			uri = new URI(convUrl.trim());
		} catch (URISyntaxException e) {
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED, "URI invalid " + convUrl);
			e.printStackTrace();
			return;
		}
		/*
		 * capture data from returned url
		 */
		List<NameValuePair> results = URLEncodedUtils.parse(uri, charSet);
		SecurityPrice newPrice = null;
		NumberFormat nf = NumberFormat.getNumberInstance();
		for (NameValuePair price : results) {
			if (price.getName().compareToIgnoreCase(Constants.STOCKTYPE) == 0) {
				newPrice = new SecurityPrice(price.getValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.CURRENCYTYPE) == 0) {
				String ticker = price.getValue();
				if (ticker.endsWith(Constants.CURRENCYTICKER))
					ticker = Constants.CURRENCYID
							+ ticker.substring(3, ticker.indexOf(Constants.CURRENCYTICKER));
				else {
					if (ticker.contains("-")) {
						ticker = Constants.CURRENCYID + ticker.substring(0, ticker.indexOf('-') + 1);
					} else
						ticker = Constants.CURRENCYID + ticker.substring(3);
				}

				newPrice = new SecurityPrice(ticker);
			}
			if (price.getName().compareToIgnoreCase(Constants.PRICETYPE) == 0) {
				if (newPrice != null) {
					String priceStr = price.getValue();
					ParsePosition pos = new ParsePosition(0);
					Number newNum = nf.parse(priceStr, pos);
					if (pos.getIndex() != priceStr.length()) {
						Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.INFO,
								"Invalid price returned for " + newPrice.getTicker() + " " + priceStr);
						newPrice.setSecurityPrice(0.0);
					} else
						newPrice.setSecurityPrice(newNum.doubleValue());
				}
			}
			if (price.getName().compareToIgnoreCase(Constants.TRADEDATETYPE) == 0) {
				String tradeDate = price.getValue();
				newPrice.setTradeDate(Integer.parseInt(tradeDate));
			}
			if (price.getName().compareToIgnoreCase(Constants.TRADECURRTYPE) == 0) {
				newPrice.setCurrency(price.getValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.TIDCMD) == 0) {
				uuid = price.getValue();
			}
			if (price.getName().compareToIgnoreCase(Constants.VOLUMETYPE) == 0) {
				newPrice.setVolume(Long.parseLong(price.getValue()));
			}
			if (price.getName().compareToIgnoreCase(Constants.HIGHTYPE) == 0) {
				String priceStr = price.getValue();
				ParsePosition pos = new ParsePosition(0);
				Number newNum = nf.parse(priceStr, pos);
				if (pos.getIndex() != priceStr.length()) {
					Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.INFO,
							"Invalid high price returned for " + newPrice.getTicker() + " " + priceStr);
					newPrice.setHighPrice(0.0);
				} else
					newPrice.setHighPrice(newNum.doubleValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.LOWTYPE) == 0) {
				String priceStr = price.getValue();
				ParsePosition pos = new ParsePosition(0);
				Number newNum = nf.parse(priceStr, pos);
				if (pos.getIndex() != priceStr.length()) {
					Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.INFO,
							"Invalid low price returned for " + newPrice.getTicker() + " " + priceStr);
					newPrice.setLowPrice(0.0);
				} else
					newPrice.setLowPrice(newNum.doubleValue());
			}
		}
		/*
		 * data extracted test for test ticker
		 */
		if (newPrice.getTicker().equals(testTicker) && testTID.equals(uuid)) {
			testTicker = "";
			testTID = "";
			return;
		}
		/*
		 * check for altered by exchange
		 */
		if (alteredTickers != null && alteredTickers.containsKey(newPrice.getTicker())) {
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED, "Ticker changed from "
					+ newPrice.getTicker() + " to " + alteredTickers.get(newPrice.getTicker()));
			newPrice.setTicker(alteredTickers.get(newPrice.getTicker()));
		}
		/*
		 * history transactions are received after the main price and can be after all
		 * tasks completed.
		 */
		if (newPrice.getSecurityPrice() == 0.0) {
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.INFO,
					"No price returned for " + newPrice.getTicker());
			return;
		}

		/*
		 * we have a price
		 */
		Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.SUMMARY,
				"Updating history price for " + newPrice.getTicker());
		String ticker = newPrice.getTicker();
		Double dRate = 1.0;
		CurrencyType securityCur = null;
		String tradeCur = newPrice.getCurrency();
		Double stockPrice = 0.0;
		Double lowPrice = 0.0;
		Double highPrice = 0.0;
		SecurityTableLine secLine = null;
		CurrencyTableLine curLine = null;
		if (newPrice.isCurrency())
			curLine = currenciesTable.get(ticker);
		else
			secLine = securitiesTable.get(ticker);

		/*
		 * check to see if trade currency is in the pseudocurrency file
		 */
		if (pseudoCurrencies.containsKey(tradeCur)) {
			PseudoCurrency line = pseudoCurrencies.get(newPrice.getCurrency());
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.SUMMARY,
					"Pseudo Currency Detected " + newPrice.getCurrency() + " for " + newPrice.getTicker());
			Double oldValue = newPrice.getSecurityPrice();
			stockPrice = oldValue * line.getMultiplier();
			oldValue = newPrice.getLowPrice();
			lowPrice = oldValue * line.getMultiplier();
			oldValue = newPrice.getHighPrice();
			highPrice = oldValue * line.getMultiplier();
			tradeCur = line.getReplacement();
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.SUMMARY,
					"Price changed from " + oldValue + " to " + stockPrice);
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.SUMMARY,
					"Currency changed from " + line.getPseudo() + " to " + line.getReplacement());
		} else {
			stockPrice = newPrice.getSecurityPrice();
			lowPrice = newPrice.getLowPrice();
			highPrice = newPrice.getHighPrice();
		} /*
			 * get currency type for trade
			 */
		CurrencyType tradeCurType = Main.context.getCurrentAccountBook().getCurrencies()
				.getCurrencyByIDString(tradeCur);
		if (tradeCurType == null)
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED, "No Price currency ");
		else
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED,
					"Price currency " + tradeCurType.getIDString());
		if (!newPrice.isCurrency()) {
			securityCur = secLine.getRelativeCurrencyType();
			if (securityCur != null)
				Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED,
						"Relative currency " + securityCur.getIDString());
			else
				Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED,
						"No Relative currency ");
		}
		int currencyDate;
		currencyDate = newPrice.getTradeDate();
		if (tradeCurType != null && securityCur != null) {
			if (!tradeCurType.equals(securityCur)) {
				/*
				 * trade currency is not security currency
				 */
				dRate = CurrencyUtil.getUserRate(tradeCurType, securityCur, currencyDate);
				Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED,
						"quote to security rate " + dRate);
			}
		}
		multiplier = Math.pow(10.0, Double.valueOf(params.getDecimal()));
		if (!newPrice.isCurrency()) {
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED,
					"before rounding" + stockPrice);
			stockPrice = Math.round(stockPrice * multiplier) / multiplier;
			lowPrice = Math.round(lowPrice * multiplier) / multiplier;
			highPrice = Math.round(highPrice * multiplier) / multiplier;
			Main.debugInst.debug("MainPriceWindow", "updateHistory", MRBDebug.DETAILED,
					"after rounding" + stockPrice);
		}
		if (stockPrice != 0.0) {
			stockPrice = stockPrice * dRate;
			if (newPrice.isCrypto())
				stockPrice = 1 / Util.safeRate(stockPrice);
			List<HistoryPrice> historyList = null;
			if (newPrice.isCurrency()) {
				historyList = curLine.getHistory();
				if (historyList == null) {
					historyList = new ArrayList<HistoryPrice>();
					curLine.setHistory(historyList);
				}
			} else {
				historyList = secLine.getHistory();
				if (historyList == null) {
					historyList = new ArrayList<HistoryPrice>();
					secLine.setHistory(historyList);
				}
			}
			HistoryPrice history = new HistoryPrice(newPrice.getTradeDate(), stockPrice, highPrice * dRate,
					lowPrice * dRate, newPrice.getVolume());
			historyList.add(history);
		}
		secPricesModel.fireTableDataChanged();
	}

	/*
	 * Failed quote
	 */
	public synchronized void failedQuote(String url) {
		String uuid = "";
		Main.debugInst.debug("MainPriceWindow", "failedQuote", MRBDebug.INFO, "URI " + url);
		URI uri = null;
		String convUrl = url.replace("^", "%5E");
		try {
			uri = new URI(convUrl.trim());
		} catch (URISyntaxException e) {
			Main.debugInst.debug("MainPriceWindow", "failedQuote", MRBDebug.SUMMARY, "URI invalid " + convUrl);
			e.printStackTrace();
			return;
		}
		List<NameValuePair> results = URLEncodedUtils.parse(uri, charSet);
		String ticker = "";
		Boolean currencyFound = false;
		QuoteSource srce=null;
		for (NameValuePair price : results) {
			if (price.getName().compareToIgnoreCase(Constants.TIDCMD) == 0) {
				uuid = price.getValue();
				for (QuoteSource srcet : QuoteSource.values()) {
					if (uuid.equals(srcet.getUuid())) {
						srce = srcet;
						break;
					}
				}
			}
			if (price.getName().compareToIgnoreCase(Constants.STOCKTYPE) == 0) {
				ticker = price.getValue();
				if (alteredTickers != null && alteredTickers.containsKey(ticker)) {
					ticker = alteredTickers.get(ticker);
					Main.debugInst.debug("MainPriceWindow", "failed ", MRBDebug.DETAILED,
							"Ticker changed from " + price.getValue() + " to " + ticker);
				}
			}
			if (price.getName().compareToIgnoreCase(Constants.CURRENCYTYPE) == 0) {
				currencyFound = true;
				ticker = price.getValue();
				Main.debugInst.debug("MainPriceWindow", "failedQuote", MRBDebug.DETAILED,
						"Currency Ticker " + ticker);
				if (ticker.endsWith(Constants.CURRENCYTICKER)) {
					switch (srce) {
					case FT:
					case FTHD:
						ticker = Constants.CURRENCYID
								+ ticker.substring(3, ticker.indexOf(Constants.CURRENCYTICKER));
						break;
					case YAHOO:
					case YAHOOHD:
					case YAHOOTD:
						if (baseCurrencyID.equals("USD"))
							ticker = Constants.CURRENCYID
									+ ticker.substring(0, ticker.indexOf(Constants.CURRENCYTICKER));
						else
							ticker = Constants.CURRENCYID
									+ ticker.substring(3, ticker.indexOf(Constants.CURRENCYTICKER));
						break;
					}
				} else {
					if (ticker.contains("-")) {
						ticker = Constants.CURRENCYID + ticker.substring(0, ticker.indexOf('-') + 1);
					} else {
						switch (srce) {
						case FT:
						case FTHD:
							ticker = Constants.CURRENCYID + ticker.substring(3);
							break;
						case YAHOO:
						case YAHOOHD:
						case YAHOOTD:
							if (baseCurrencyID.equals("USD") && !uuid.isEmpty())
								ticker = Constants.CURRENCYID + ticker.substring(0, 2);
							else
								ticker = Constants.CURRENCYID + ticker.substring(3);
							break;
						default:
							break;

						}
					}
				}
			}
		}
		if (runtype != Constants.MANUALRUN && runtype != 0) {
			if (currencyFound)
				Main.context.showURL("moneydance:setprogress?meter=0&label=Quote Loader price " + ticker
						+ " failed currency");
			else
				Main.context.showURL("moneydance:setprogress?meter=0&label=Quote Loader price " + ticker
						+ " failed stock");
		}
		/*
		 * if test of ticker, display message and exit
		 */
		if (ticker.equals(testTicker) && testTID.equals(uuid)) {
			testTicker = "";
			testTID = "";
			String message = "Test of security " + ticker + " failed.";
			JOptionPane.showMessageDialog(null, message);
			return;
		}
		/*
		 * if completed set, ignore message
		 */
		if (completed) {
			Main.debugInst.debug("MainPriceWindow", "failedQuote", MRBDebug.INFO, "Late message");
			return;
		}
		errorsFound = true;
		if (errorTickers != null)
			errorTickers.add(ticker);
		if (listener != null)
			listener.failed(ticker, uuid);
	}

	public synchronized void doneQuote(String url) {
		int totalQuotes = 0;
		int successful = 0;
		int failed = 0;
		/*
		 * if completed set, ignore message
		 */
		if (completed) {
			Main.debugInst.debug("MainPriceWindow", "doneQuote", MRBDebug.INFO, "Late message");
			return;
		}
		String uuid = "";
		URI uri = null;
		try {
			uri = new URI(url.trim());
		} catch (URISyntaxException e) {
			Main.debugInst.debug("MainPriceWindow", "doneQuote", MRBDebug.DETAILED, "URI invalid " + url);
			e.printStackTrace();
			return;
		}
		List<NameValuePair> results = URLEncodedUtils.parse(uri, charSet);
		for (NameValuePair price : results) {
			if (price.getName().compareToIgnoreCase(Constants.TOTALTYPE) == 0) {
				totalQuotes = Integer.parseInt(price.getValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.OKTYPE) == 0) {
				successful = Integer.parseInt(price.getValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.ERRTYPE) == 0) {
				failed = Integer.parseInt(price.getValue());
			}
			if (price.getName().compareToIgnoreCase(Constants.TIDCMD) == 0) {
				uuid = price.getValue();
			}
		}
		Main.debugInst.debug("MainPriceWindow", "doneQuote", MRBDebug.INFO, "Finished quote " + uuid);
		if (listener != null)
			listener.done(uuid, totalQuotes, successful, failed);

	}

	/*
	 * Listener for when tasks finished
	 */
	@Override
	public synchronized void TasksCompleted() {
		if (!completed) {
			Main.debugInst.debug("MainPriceWindow", "TaskCompleted", MRBDebug.INFO, "Tasks Completed");
			completed = true;
			boolean extensionError = false;
			boolean currencyError = false;
			if (processSecurity) {
				for (Entry<String, SecurityTableLine> entry : securitiesTable.entrySet()) {
					SecurityTableLine line = entry.getValue();
					String entryTicker = entry.getKey();
					if (!entryTicker.contains(Constants.TICKEREXTID))
						continue;
					String primTicker = entryTicker.substring(0, entryTicker.indexOf(Constants.TICKEREXTID));
					SecurityTableLine altLine = securitiesTable.get(primTicker);
					if (altLine == null) {
						Main.debugInst.debug("MainPriceWindow", "TasksCompleted", MRBDebug.DETAILED,
								"Ticker " + entryTicker + " does not have an entry for " + primTicker);
						extensionError = true;
						line.setTickerStatus(Constants.TASKFAILED);
						continue;
					}
					CurrencyType extCur = baseCurrency;
					CurrencyType tickerCur = baseCurrency;
					if (line.getDifferentCur())
						extCur = line.getRelativeCurrencyType();
					if (altLine.getDifferentCur())
						tickerCur = altLine.getRelativeCurrencyType();
					if (extCur != null && extCur != tickerCur) {
						Main.debugInst.debug("MainPriceWindow", "TasksCompleted", MRBDebug.DETAILED,
								"Ticker " + entryTicker + " has a different currency");
						currencyError = true;
						line.setTickerStatus(Constants.TASKFAILED);
						continue;
					}
					line.setTickerStatus(Constants.TASKCOMPLETED);
					line.setNewPrice(altLine.getNewPrice());
					line.setAmtChg(altLine.getAmtChg());
					line.setPercentChg(altLine.getPercentChg());
					line.setTradeDate(altLine.getTradeDate());
					line.setTradeCur(altLine.getTradeCur());
					line.setQuotedPrice(altLine.getQuotedPrice());
					line.setHistory(altLine.getHistory());
					line.setVolume(altLine.getVolume());
				}
			}
			if (runtype == Constants.MANUALRUN) {
				tasksProgress.setValue(100);
				String mess = "";
				if (listener != null)
					mess += listener.getSuccessful();
				mess += " Prices Loaded";
				if (listener != null) {
					if (listener.getFailed() > 0)
						mess += " " + listener.getFailed() + " errors";
				}
				if (extensionError)
					mess += ", prices missing for extended Tickers";
				if (currencyError)
					mess += ", extended ticker has different currency";
				JOptionPane.showMessageDialog(null, mess, "Get Completed", JOptionPane.INFORMATION_MESSAGE);
				tasksProgress.setVisible(false);
				buttonsPanel.remove(tasksProgress);
				if (tabs.getSelectedIndex() == 0) {
					getPricesBtn.setEnabled(true);
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SAME)
						getRatesBtn.setEnabled(true);
				}
				if (tabs.getSelectedIndex() == 1) {
					if (params.getDisplayOption() == Constants.CurrencyDisplay.SEPARATE)
						getRatesBtn.setEnabled(true);
				}
				if (processSecurity)
					secPricesModel.fireTableDataChanged();
				if (processCurrency)
					curRatesModel.fireTableDataChanged();
				this.revalidate();
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Main.context.showURL("moneydance:fmodule:" + Constants.PROGRAMNAME + ":"
								+ Constants.MANUALDONECMD);
					}
				});

			}
			if (runtype != Constants.MANUALRUN && runtype != 0) {
				if (errorsFound && !Main.secondRunRequired) {
					JOptionPane.showMessageDialog(null,
							"Errors found on automatic run.  Look at 'Price Date' to determine which lines have not been updated",
							"Quote Loader", JOptionPane.ERROR_MESSAGE);
					errorsFound = false;
				}
				Main.debugInst.debug("AutomaticRun", "AutomaticRun", MRBDebug.DETAILED, "set saveall");
				if (Main.secondRunRequired) {
					if (processCurrency)
						curRatesModel.selectAll(true);
					main.errorTickers = errorTickers;
					Main.debugInst.debug("AutomaticRun", "AutomaticRun", MRBDebug.DETAILED, "save data");
					save();
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Main.context.showURL("moneydance:fmodule:" + Constants.PROGRAMNAME + ":"
									+ Constants.RUNSECONDRUNCMD);
						}
					});
				}
				else {
					if (processCurrency)
						curRatesModel.selectAll(true);
					if (processSecurity)
						secPricesModel.selectAll(true);
					main.errorTickers = errorTickers;
					save();
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Main.context.showURL("moneydance:fmodule:" + Constants.PROGRAMNAME + ":"
									+ Constants.AUTODONECMD);
						}
					});
				}
			}
		}
	}

	@Override
	public synchronized void Update() {
		Main.debugInst.debug("MainPriceWindow", "Update", MRBDebug.DETAILED, "Progress Bar Updated");
		secPricesModel.fireTableDataChanged();
		this.revalidate();
	}

	public synchronized boolean checkProgress() {
		for (SecurityTableLine line : securitiesTable.values()) {
			if (line.getTickerStatus() == Constants.TASKSTARTED) {
				Main.debugInst.debug("MainPriceWindow", "checkProgress", MRBDebug.SUMMARY,
						"Quote " + line.getTicker() + " has not finished");
				return false;
			}
		}
		return true;
	}

	public synchronized void closeQuotes() {
		for (SecurityTableLine line : securitiesTable.values()) {
			if (line.getTickerStatus() == Constants.TASKSTARTED) {
				if (listener != null)
					listener.failed(line.getTicker());
			}
		}

	}

	/*
	 * preferences
	 */
	protected void setPreferences() {
		iFRAMEWIDTH = Main.preferences
				.getInt(Constants.PROGRAMNAME + "." + Constants.CRNTFRAMEWIDTH + "." + selectedTab, -9999);
		iFRAMEDEPTH = Main.preferences
				.getInt(Constants.PROGRAMNAME + "." + Constants.CRNTFRAMEDEPTH + "." + selectedTab, -9999);
		if (iFRAMEWIDTH == -9999) {
			iFRAMEWIDTH = Main.preferences.getInt(Constants.PROGRAMNAME + "." + Constants.CRNTFRAMEWIDTH,
					Constants.FRAMEWIDTH);
			iFRAMEDEPTH = Main.preferences.getInt(Constants.PROGRAMNAME + "." + Constants.CRNTFRAMEDEPTH,
					Constants.FRAMEWIDTH);
		}
	}

	protected void updatePreferences(Dimension objDim) {
		Main.preferences.put(Constants.PROGRAMNAME + "." + Constants.CRNTFRAMEWIDTH + "." + selectedTab,
				objDim.width);
		Main.preferences.put(Constants.PROGRAMNAME + "." + Constants.CRNTFRAMEDEPTH + "." + selectedTab,
				objDim.height);
		Main.preferences.isDirty();
	}

	public class CloseAction extends AbstractAction {
		private Window window;

		public CloseAction(Window window) {
			this.window = window;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (window == null)
				return;
			window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
		}
	}

	@Override
	public void currencyTableModified(CurrencyTable paramCurrencyTable) {
		Main.debugInst.debug("MainPriceWindow", "currencyTableModified", MRBDebug.DETAILED, "");
		/*
		 * do not update if in middle of update
		 */
		if (Main.isUpdating)
			return;
		securitiesTable.clear();
		currenciesTable.clear();
		loadAccounts(Main.context.getRootAccount());
		baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
		baseCurrencyID = baseCurrency.getIDString();
		loadCurrencies(Main.context.getCurrentAccountBook());
		if (secPricesModel != null) {
			secPricesModel.resetData(securitiesTable);
			secPricesModel.fireTableDataChanged();
		}
		if (curRatesModel != null) {
			curRatesModel.resetData(currenciesTable);
			curRatesModel.fireTableDataChanged();
		}
	}

	@Override
	public void accountModified(Account modifiedAccount) {
		Main.debugInst.debug("MainPriceWindow", "accountModified", MRBDebug.DETAILED,
				"Account - " + modifiedAccount.getAccountName());
		/*
		 * do not update if in middle of update
		 */
		if (Main.isUpdating)
			return;
		securitiesTable.clear();
		currenciesTable.clear();
		loadAccounts(Main.context.getRootAccount());
		baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
		baseCurrencyID = baseCurrency.getIDString();
		loadCurrencies(Main.context.getCurrentAccountBook());
		if (secPricesModel != null) {
			secPricesModel.resetData(securitiesTable);
			secPricesModel.fireTableDataChanged();
		}
		if (curRatesModel != null) {
			curRatesModel.resetData(currenciesTable);
			curRatesModel.fireTableDataChanged();
		}

	}

	@Override
	public void accountBalanceChanged(Account modifiedAccount) {
		Main.debugInst.debug("MainPriceWindow", "accountBalanceChanged", MRBDebug.DETAILED,
				"Account - " + modifiedAccount.getAccountName());
		/*
		 * do not update if in middle of update
		 */
		if (Main.isUpdating)
			return;
		securitiesTable.clear();
		currenciesTable.clear();
		loadAccounts(Main.context.getRootAccount());
		baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
		baseCurrencyID = baseCurrency.getIDString();
		loadCurrencies(Main.context.getCurrentAccountBook());
		if (secPricesModel != null) {
			secPricesModel.resetData(securitiesTable);
			secPricesModel.fireTableDataChanged();
		}
		if (curRatesModel != null) {
			curRatesModel.resetData(currenciesTable);
			curRatesModel.fireTableDataChanged();
		}
	}

	@Override
	public void accountDeleted(Account parentAccount, Account deletedAccount) {
		Main.debugInst.debug("MainPriceWindow", "accountDeleted", MRBDebug.DETAILED,
				"Account - " + deletedAccount.getAccountName());
		/*
		 * do not update if in middle of update
		 */
		if (Main.isUpdating)
			return;
		securitiesTable.clear();
		currenciesTable.clear();
		loadAccounts(Main.context.getRootAccount());
		baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
		baseCurrencyID = baseCurrency.getIDString();
		loadCurrencies(Main.context.getCurrentAccountBook());
		if (secPricesModel != null) {
			secPricesModel.resetData(securitiesTable);
			secPricesModel.fireTableDataChanged();
		}
		if (curRatesModel != null) {
			curRatesModel.resetData(currenciesTable);
			curRatesModel.fireTableDataChanged();
		}
	}

	@Override
	public void accountAdded(Account parentAccount, Account newAccount) {
		Main.debugInst.debug("MainPriceWindow", "accountAdded", MRBDebug.DETAILED,
				"Account - " + newAccount.getAccountName());
		/*
		 * do not update if in middle of update
		 */
		if (Main.isUpdating)
			return;
		securitiesTable.clear();
		currenciesTable.clear();
		loadAccounts(Main.context.getRootAccount());
		baseCurrency = Main.context.getCurrentAccountBook().getCurrencies().getBaseType();
		baseCurrencyID = baseCurrency.getIDString();
		loadCurrencies(Main.context.getCurrentAccountBook());
		if (secPricesModel != null) {
			secPricesModel.resetData(securitiesTable);
			secPricesModel.fireTableDataChanged();
		}
		if (curRatesModel != null) {
			curRatesModel.resetData(currenciesTable);
			curRatesModel.fireTableDataChanged();
		}
	}
}
