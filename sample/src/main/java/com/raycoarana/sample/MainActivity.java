/*
    Copyright 2016 Rayco Araña

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.raycoarana.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.raycoarana.sample.app.SampleApplication;
import com.raycoarana.tesela.R;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity implements MainView {

    @Inject
    protected MainPresenter mPresenter;

    private ProgressBar mLoadingView;
    private TextView mMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        injectMembers();
        bindViews();

        mPresenter.onInitialize(this);
    }

    private void injectMembers() {
        ((SampleApplication) getApplication()).getApplicationComponent().inject(this);
    }

    private void bindViews() {
        mLoadingView = (ProgressBar) findViewById(R.id.progress);
        mMessageView = (TextView) findViewById(R.id.message);
    }

    @Override
    public void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
    }

    @Override
    public void showMessage(String message) {
        mMessageView.setVisibility(View.VISIBLE);
        mMessageView.setText(message);
    }

    @Override
    public void hideMessage() {
        mMessageView.setVisibility(View.GONE);
    }

    @Override
    public boolean isBig() {
        return false;
    }

}
