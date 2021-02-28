/*
 * Copyright (c) 2021, Michael Bray.  All rights reserved.
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
package com.moneydance.modules.features.reportwriter.factory;

import java.util.List;
import java.util.SortedMap;

import com.infinitekind.moneydance.model.CurrencyTable;
import com.infinitekind.moneydance.model.CurrencyType;
import com.moneydance.modules.features.reportwriter.Constants;
import com.moneydance.modules.features.reportwriter.Main;
import com.moneydance.modules.features.reportwriter.RWException;
import com.moneydance.modules.features.reportwriter.databeans.SecurityBean;
import com.moneydance.modules.features.reportwriter.view.DataDataRow;
import com.moneydance.modules.features.reportwriter.view.DataParameter;

public class SecurityFactory {
	private DataDataRow dataParams;
	private CurrencyTable curTable;
	private List<CurrencyType> allSecurities;
	private List<String> selectedSecurities = null;
	private SortedMap<String, DataParameter> map;

	public SecurityFactory(DataDataRow dataParamsp, OutputFactory output) throws RWException {
		dataParams = dataParamsp;
		map = dataParams.getParameters();
		if (map.containsKey(Constants.PARMSECURITY))
			selectedSecurities = map.get(Constants.PARMSECURITY).getList();
		curTable = Main.book.getCurrencies();
		allSecurities = curTable.getAllCurrencies();
		for (CurrencyType cur : allSecurities) {
			if (cur.getCurrencyType() != CurrencyType.Type.SECURITY)
				continue;
			if (selectedSecurities != null && !selectedSecurities.contains(cur.getUUID()))
				continue;
			if (map.containsKey(Constants.PARMSELSECURITY) && selectedSecurities == null)
				continue;
			SecurityBean bean = new SecurityBean();
			bean.setSelection(output.getSelection());
			bean.setSecurity(cur);
			bean.populateData();
			try {
				output.writeRecord(bean);
			} catch (RWException e) {
				throw e;
			}
		}
	}
}
