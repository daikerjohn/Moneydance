package com.moneydance.modules.features.loadsectrans;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;

public class Parameters implements Serializable{
	/*
	 * Static and transient fields are not stored 
	 */
	private static final long serialVersionUID = 1L;
	private transient AccountBook abCurAcctBook;
	private transient File fiCurFolder;
	private transient FileInputStream fiCurInFile;
	private transient FileOutputStream fiCurOutFile;
	private transient String strFileName;
	/*
     * The following fields are stored
     */

	private String strTicker;
	private String strValue;
	private String strShares;
	private String strPrice;
	private String strDate;
	private String strReference;
	private String strDesc;
	private boolean bExch;
	private List<FieldLine> listFieldLines;

	public static final char INVALID_CHARS[] = { '\\', '/', ':', '*', '?', '"', '<', '>', '|', '[', ']', '\'', ';', '=', ',' };
	private static final char SANITIZED_CHAR = '_';
	public static String sanitizeFileName(String filename, char substitute) {

		for (int i = 0; i < INVALID_CHARS.length; i++) {
			if (-1 != filename.indexOf(INVALID_CHARS[i])) {
				filename = filename.replace(INVALID_CHARS[i], substitute);
			}
		}

		return filename;
	}
	public Parameters() {
		this("SecureTranLoad.bpam");
	}
	public Parameters(String accountName) {
		/*
		 * determine if file already exists
		 */
		abCurAcctBook = Main.context.getRootAccount().getBook();
		fiCurFolder = abCurAcctBook.getRootFolder();
		accountName = sanitizeFileName(accountName, SANITIZED_CHAR);

		strFileName = Paths.get(fiCurFolder.getAbsolutePath(), "SecureTranLoad-"+accountName+".bpam").toString();
		//strFileName = fiCurFolder.getAbsolutePath()+"\\" + accountName;

		try {
			fiCurInFile = new FileInputStream(strFileName);
			ObjectInputStream ois = new ObjectInputStream(fiCurInFile);
			/*
			 * file exists, copy temporary object to this object
			 */
			Parameters objTemp = (Parameters) ois.readObject();
			this.strTicker = objTemp.strTicker;
			this.strValue = objTemp.strValue;
			this.strShares = objTemp.strShares;
			this.strPrice = objTemp.strPrice;
			this.strDate = objTemp.strDate;
			this.strReference = objTemp.strReference;
			this.strDesc = objTemp.strDesc;
			this.bExch = objTemp.bExch;
			this.listFieldLines = objTemp.listFieldLines;
			fiCurInFile.close();
		}
		catch (IOException | ClassNotFoundException ioException) {
			/*
			 * file does not exist, initialize fields
			 */
			listFieldLines = new ArrayList<FieldLine>();
			strTicker = "";
			strValue = "";
			strShares = "";
			strPrice = "";
			strDate = "";
			strReference = "";
			strDesc = "";
			bExch = false;
			/*
			 * create the file
			 */
			try {
				fiCurOutFile = new FileOutputStream(strFileName);
				ObjectOutputStream oos = new ObjectOutputStream(fiCurOutFile);
				oos.writeObject(this);
				fiCurOutFile.close();
			}
			catch(IOException i)
			{
				i.printStackTrace();
			}
		}		
	}
	public String getTicker() {
		return strTicker;
	}
	public String getValue() {
		return strValue;
	}
	public String getShares() {
		return strShares;
	}
	public String getPrice() {
		return strPrice;
	}
	public String getDate() {
		return strDate;
	}
	public String getReference() {
		return strReference;
	}
	public String getDesc() {
		return strDesc;
	}
	public boolean getExch() {
		return bExch;
	}
	public List<FieldLine> getFields() {
		return listFieldLines;
	}
	public void setTicker(String strTickerp) {
		strTicker = strTickerp;
	}
	public void setValue(String strValuep) {
		strValue = strValuep;
	}
	public void setShares(String strSharesp) {
		strShares = strSharesp;
	}
	public void setPrice(String strPricep) {
		strPrice = strPricep;
	}
	public void setDate(String strDatep) {
		strDate = strDatep;
	}
	public void setReference(String strReferencep) {
		strReference = strReferencep;
	}
	public void setDesc(String strDescp) {
		strDesc = strDescp;
	}
	public void setExch(boolean bExchp) {
		bExch = bExchp;
	}
	public void addField(String strType, String strAcctName, Account acct, int iTranType) {
		FieldLine objLine = new FieldLine(strType, strAcctName, acct, iTranType);
		if (listFieldLines == null)
			listFieldLines = new ArrayList<FieldLine>();
		listFieldLines.add(objLine);
	}

	public void updateAccount(String strType, String strAcctName, Account acct){
		for (FieldLine objLine :listFieldLines) {
			if (objLine.getType().equals(strType)) {
				objLine.setAccount(strAcctName,acct);
			}	
		}
	}

	public void updateTransType(String strType,int iTranType){
		for (FieldLine objLine :listFieldLines) {
			if (objLine.getType().equals(strType)) {
				objLine.setTranType(iTranType);
			}	
		}
	}


	public void deleteField(String strType){
		for (FieldLine objLine :listFieldLines) {
			if (objLine.getType().equals(strType)) {
				listFieldLines.remove(objLine);
				break;
			}
		}
	}
	public List<FieldLine> getLines() {
		return listFieldLines;
	}
	
	public void save() {
		/*
		 * Save the parameters into the specified file
		 */
		try {
			fiCurOutFile = new FileOutputStream(strFileName);
			ObjectOutputStream oos = new ObjectOutputStream(fiCurOutFile);
			oos.writeObject(this);
			oos.close();
			fiCurOutFile.close();
			Gson gson = new Gson();
			String json = gson.toJson(this);
			System.err.println(json);
		}
		catch(IOException i)
		{
			i.printStackTrace();
		}
	}
	public boolean isDefined(String strType){
		for (FieldLine objLine: listFieldLines) {
			if (objLine.getType().equals(strType))
				return true;			
		}
		return false;
	}
	public boolean requiresTicker(String strType) {
		for (FieldLine objLine: listFieldLines) {
			if (objLine.getType().equals(strType)) {
				switch (Constants.TRANSTYPES[objLine.getTranType()]) {
				case Constants.SECURITY_COST:
				case Constants.SECURITY_DIVIDEND:
				case Constants.SECURITY_INCOME:
				case Constants.SECURITY_BUY:
				case Constants.SECURITY_BUY_XFER:
				case Constants.SECURITY_SELL:
				case Constants.SECURITY_SELL_XFER:
					return true;
				default : 
					return false;
				}
			}
		}
		return false;					
	}
	public FieldLine matchType(String strType){
		for (FieldLine objLine: listFieldLines) {
			if (objLine.getType().equals(strType))
				return objLine;			
		}
		return null;
	}}
