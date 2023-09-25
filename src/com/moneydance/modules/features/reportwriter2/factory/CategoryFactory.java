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
package com.moneydance.modules.features.reportwriter2.factory;

import java.util.List;
import java.util.SortedMap;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.Account.AccountType;
import com.infinitekind.moneydance.model.AccountUtil;
import com.infinitekind.moneydance.model.AcctFilter;
import com.moneydance.modules.features.reportwriter2.Constants;
import com.moneydance.modules.features.reportwriter2.Main;
import com.moneydance.modules.features.reportwriter2.RWException;
import com.moneydance.modules.features.reportwriter2.databeans.CategoryBean;
import com.moneydance.modules.features.reportwriter2.view.DataParameter;
import com.moneydance.modules.features.reportwriter2.view.tables.DataDataRow;

public class CategoryFactory implements AcctFilter {
	private DataDataRow dataParams;
	private List<Account> allCats;
	private List<String> selectedCats = null;
	private SortedMap<String, DataParameter> map;
	private Boolean allExpense = false;
	private Boolean allIncome = false;
	private Boolean specified = false;

	public CategoryFactory(DataDataRow dataParamsp, OutputFactory output) throws RWException {
		dataParams = dataParamsp;
		map = dataParams.getParameters();
		if (map.containsKey(Constants.PARMCATEGORIES))
			selectedCats = map.get(Constants.PARMCATEGORIES).getList();
		if (selectedCats != null)
			specified = true;
		else {
			if (map.containsKey(Constants.PARMINCOME))
				allIncome = true;
			if (map.containsKey(Constants.PARMEXPENSE))
				allExpense = true;
		}
		allCats = AccountUtil.allMatchesForSearch(Main.book, this);
		for (Account acct : allCats) {
			CategoryBean bean = new CategoryBean();
			bean.setSelection(output.getSelection());
			bean.setCategory(acct);
			bean.populateData();
			try {
				output.writeRecord(bean);
			} catch (RWException e) {
				throw e;
			}
		}
	}

	@Override
	public boolean matches(Account paramAccount) {
		if (!(paramAccount.getAccountType() == AccountType.INCOME
				|| paramAccount.getAccountType() == AccountType.EXPENSE))
			return false;
		if (specified) {
			if (selectedCats.contains(paramAccount.getUUID()))
				return true;
			return false;
		}
		if (map.containsKey(Constants.PARMSELCAT) && !allIncome && !allExpense)
			return false;
		if (allIncome && paramAccount.getAccountType() == AccountType.INCOME)
			return true;
		if (allExpense && paramAccount.getAccountType() == AccountType.EXPENSE)
			return true;
		if (!specified && !allIncome && !allExpense)
			return true;
		return false;
	}

	@Override
	public String format(Account paramAccount) {
		return paramAccount.getUUID();
	}
}
