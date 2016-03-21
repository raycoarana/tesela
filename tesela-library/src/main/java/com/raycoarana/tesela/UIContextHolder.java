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
package com.raycoarana.tesela;

import java.lang.ref.WeakReference;

public class UIContextHolder<V> {

    private final WeakReference<V> mViewReference;

    public static <V> UIContextHolder<V> from(V view) {
        return new UIContextHolder<>(view);
    }

    private UIContextHolder(V view) {
        mViewReference = new WeakReference<>(view);
    }

    public V get() {
        return mViewReference.get();
    }

    public boolean isValid() {
        return mViewReference.get() != null;
    }

}
