/**
 * Copyright 2014 AnjLab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anjlab.android.iab.v3.sample2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.util.List;

public class MainActivity extends Activity {
	// SAMPLE APP CONSTANTS
	private static final String ACTIVITY_NUMBER = "activity_num";
	private static final String LOG_TAG = "iabv3";

    // PRODUCT & SUBSCRIPTION IDS
    private static final String PRODUCT_ID = "com.anjlab.test.iab.s2.p5";
    private static final String SUBSCRIPTION_ID = "com.anjlab.test.iab.subs1";
    private static final String LICENSE_KEY = null; // PUT YOUR MERCHANT KEY HERE;
    // put your Google merchant id here (as stated in public profile of your Payments Merchant Center)
    // if filled library will provide protection against Freedom alike Play Market simulators
    private static final String MERCHANT_ID=null;

	private BillingProcessor bp;
	private boolean readyToPurchase = false;


	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		TextView title = (TextView)findViewById(R.id.titleTextView);
		title.setText(String.format(getString(R.string.title), getIntent().getIntExtra(ACTIVITY_NUMBER, 1)));

        if(!BillingProcessor.isIabServiceAvailable(this)) {
            showToast("In-app billing service is unavailable, please upgrade Android Market/Play to version >= 3.9.16");
        }

        bp = new BillingProcessor(this, LICENSE_KEY, new BillingProcessor.IBillingHandler() {
            @Override
            public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
				showToast("onProductPurchased: " + productId);
                updateTextViews();
            }
            @Override
            public void onBillingError(int errorCode, @Nullable Throwable error) {
                showToast("onBillingError: " + Integer.toString(errorCode));
            }
            @Override
            public void onBillingInitialized() {
				showToast("onBillingInitialized");
                readyToPurchase = true;
                updateTextViews();
            }
            @Override
            public void onPurchaseHistoryRestored() {
                showToast("onPurchaseHistoryRestored");
                for(String sku : bp.listOwnedProducts())
                    Log.d(LOG_TAG, "Owned Managed Product: " + sku);
                for(String sku : bp.listOwnedSubscriptions())
                    Log.d(LOG_TAG, "Owned Subscription: " + sku);
                updateTextViews();
            }
        });
    }

	@Override
	protected void onResume() {
		super.onResume();

		updateTextViews();
	}

	@Override
    public void onDestroy() {
        if (bp != null)
            bp.release();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateTextViews() {
        TextView text = (TextView)findViewById(R.id.productIdTextView);
        text.setText(String.format("%s is%s purchased", PRODUCT_ID, bp.isPurchased(PRODUCT_ID) ? "" : " not"));
        text = (TextView)findViewById(R.id.subscriptionIdTextView);
        text.setText(String.format("%s is%s subscribed", SUBSCRIPTION_ID, bp.isSubscribed(SUBSCRIPTION_ID) ? "" : " not"));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void onClick(View v) {
        if (!readyToPurchase) {
            showToast("Billing not initialized.");
            return;
        }
        switch (v.getId()) {
            case R.id.purchaseButton:
                bp.purchase(this,PRODUCT_ID);
                break;
            case R.id.consumeButton:
                bp.consumePurchaseAsync(PRODUCT_ID, new BillingProcessor.IPurchasesResponseListener()
                {
                    @Override
                    public void onPurchasesSuccess()
                    {
                        showToast("Successfully consumed");
                    }

                    @Override
                    public void onPurchasesError()
                    {
                        showToast("Not consumed");
                    }
                });
                updateTextViews();
                break;
            case R.id.productDetailsButton:
				bp.getPurchaseListingDetailsAsync(PRODUCT_ID, new BillingProcessor.ISkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@Nullable List<SkuDetails> products) {
                        if (products != null && !products.isEmpty()) {
                            showToast(products.get(0).toString());
                        } else {
                            showToast("Failed to load SKU details");
                        }
                    }

                    @Override
                    public void onSkuDetailsError(String error) {
                        showToast(error);
                    }
                });

                break;
            case R.id.subscribeButton:
                bp.subscribe(this,SUBSCRIPTION_ID);
                break;
            case R.id.updateSubscriptionsButton:
                bp.loadOwnedPurchasesFromGoogleAsync(new BillingProcessor.IPurchasesResponseListener() {
                    @Override
                    public void onPurchasesSuccess()
                    {
                        showToast("Subscriptions updated.");
                        updateTextViews();
                    }

                    @Override
                    public void onPurchasesError()
                    {
                        showToast("Subscriptions update eroor.");
                        updateTextViews();
                    }
                });

                break;
            case R.id.subsDetailsButton:
				 bp.getSubscriptionListingDetailsAsync(SUBSCRIPTION_ID, new BillingProcessor.ISkuDetailsResponseListener()
                 {
                    @Override
                    public void onSkuDetailsResponse(@Nullable final List<SkuDetails> products) {
                        showToast(products != null ? products.toString() : "Failed to load subscription details");
                    }

                    @Override
                    public void onSkuDetailsError(String string) {
                        showToast(string);
                    }
                });

				break;
			case R.id.launchMoreButton:
				startActivity(new Intent(this, MainActivity.class).putExtra(ACTIVITY_NUMBER, getIntent().getIntExtra(ACTIVITY_NUMBER, 1) + 1));
				break;
            default:
                break;
        }
    }

}
