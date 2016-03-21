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

import com.raycoarana.sample.usecase.HardWorkCommand;
import com.raycoarana.tesela.annotations.Background;
import com.raycoarana.tesela.annotations.UI;
import com.raycoarana.tesela.annotations.ViewReference;

import javax.inject.Inject;

public class MainPresenter {

    private final HardWorkCommand mHardWorkCommand;

    @ViewReference
    protected WeakMainView mView;

    @Inject
    public MainPresenter(HardWorkCommand hardWorkCommand) {
        mHardWorkCommand = hardWorkCommand;
    }

    public void onInitialize(MainView view) {
        mView = WeakMainView.of(view);

        showLoading();
        loadData();
    }

    @UI
    protected void showLoading() {
        mView.showLoading();
        mView.hideMessage();
    }

    @Background
    protected void loadData() {
        String result = mHardWorkCommand.execute();
        showMessage(result);
    }

    @UI
    protected void showMessage(String message) {
        mView.hideLoading();
        mView.showMessage(message);
    }

}
