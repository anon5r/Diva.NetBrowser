package net.diva.browser.settings;

import java.io.IOException;

import net.diva.browser.R;
import net.diva.browser.db.LocalStore;
import net.diva.browser.service.ServiceClient;
import android.content.Context;
import android.content.Intent;

public class ConfigActivationIndividual extends ConfigMultiChoice {
	public ConfigActivationIndividual(Context context) {
		super(context, R.array.individual_setting_keys, R.array.individual_setting_names, true,
				R.string.category_activation_individual, R.string.summary_activation_individual);
	}

	@Override
	protected Boolean apply(ServiceClient service, LocalStore store, Intent data) throws IOException {
		service.activateIndividual(m_keys, m_values);
		saveToLocal();
		return Boolean.TRUE;
	}
}
