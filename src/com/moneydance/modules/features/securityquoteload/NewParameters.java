/*
 * Copyright (c) 2018, Michael Bray.  All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

public class NewParameters {
	private int noDecimals;
	private int newNoDecimals;
	private boolean includeZero;
	private boolean includeCurrency;
	private boolean addVolume;
	private boolean history;
	private boolean export;
	private boolean exportAuto;
	private boolean roundPrices;
	private String exportFolder;
	private List<NewAccountLine> listAccounts;

	public  NewParameters(){
		noDecimals = 0;
		newNoDecimals = -1;
		roundPrices = false;
		includeZero = false;
		includeCurrency = false;
		addVolume = false;
		listAccounts = new ArrayList<>();
	}
	/**
	 * @return the noDecimals
	 */
	public int getNoDecimals() {
		return noDecimals;
	}
	/**
	 * @return the includeZero
	 */
	public boolean isIncludeZero() {
		return includeZero;
	}
	/**
	 * @return the addVolume
	 */
	public boolean isAddVolume() {
		return addVolume;
	}
	/**
	 * @return the history
	 */
	public boolean isHistory() {
		return history;
	}
	/**
	 * @return the includeCurrency
	 */
	public boolean isIncludeCurrency() {
		return includeCurrency;
	}
	/**
	 * @return the listAccounts
	 */
	public List<NewAccountLine> getListAccounts() {
		return listAccounts;
	}
	/**
	 * @param noDecimals the noDecimals to set
	 */
	public void setNoDecimals(int noDecimals) {
		this.noDecimals = noDecimals;
	}
	/**
	 * @param includeZero the includeZero to set
	 */
	public void setIncludeZero(boolean includeZero) {
		this.includeZero = includeZero;
	}
	/**
	 * @param includeCurrency the includeCurrency to set
	 */
	public void setIncludeCurrency(boolean includeCurrency) {
		this.includeCurrency = includeCurrency;
	}
	/**
	 * @param listAccounts the listAccounts to set
	 */
	public void setListAccounts(List<NewAccountLine> listAccounts) {
		this.listAccounts = listAccounts;
	}
	/**
	 * @param addVolume the addVolume to set
	 */
	public void setAddVolume(boolean addVolume) {
		this.addVolume = addVolume;
	}
	/**
	 * @param history the history to set
	 */
	public void setHistory(boolean history) {
		this.history = history;
	}
	public boolean isExport() {
		return export;
	}
	public void setExport(boolean export) {
		this.export = export;
	}
	public boolean isExportAuto() {
		return exportAuto;
	}
	public void setExportAuto(boolean exportAuto) {
		this.exportAuto = exportAuto;
	}
	public String getExportFolder() {
		return exportFolder;
	}
	public void setExportFolder(String exportFolder) {
		this.exportFolder = exportFolder;
	}
	public int getNewNoDecimals() {
		return newNoDecimals;
	}
	public void setNewNoDecimals(int newNoDecimals) {
		this.newNoDecimals = newNoDecimals;
	}
	public boolean isRoundPrices() {
		return roundPrices;
	}
	public void setRoundPrices(boolean roundPrices) {
		this.roundPrices = roundPrices;
	}
	
}
