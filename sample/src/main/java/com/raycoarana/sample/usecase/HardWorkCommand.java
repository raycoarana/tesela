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
package com.raycoarana.sample.usecase;

import android.util.Log;

import javax.inject.Inject;

public class HardWorkCommand {

    private static final String TAG = "HardWorkCommand";

    @Inject
    public HardWorkCommand() {

    }

    public String execute() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return "HelloWorld!";
    }

}
