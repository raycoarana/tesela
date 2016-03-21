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
package com.raycoarana.sample.tesela;

import android.os.Handler;
import android.os.Looper;

import com.raycoarana.tesela.TeselaExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class SampleTeselaExecutor implements TeselaExecutor {

    private final Handler mMainThreadHandler;
    private final Executor mBackgroundExecutor;

    @Inject
    public SampleTeselaExecutor() {
        mMainThreadHandler = new Handler();
        mBackgroundExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean isMainCurrentThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    @Override
    public void executeInUIThread(Runnable runnable) {
        mMainThreadHandler.post(runnable);
    }

    @Override
    public void executeInBackground(Runnable runnable) {
        executeInBackground(runnable, null);
    }

    @Override
    public void executeInBackground(Runnable runnable, String tag) {
        mBackgroundExecutor.execute(runnable);
    }

}
