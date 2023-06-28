/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.pm;

import android.annotation.IdRes;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.DataClass;

/**
 * Information about an attribution declared by a package. This corresponds to the information
 * collected from the AndroidManifest.xml's &lt;attribution&gt; tags.
 */
@DataClass(genHiddenConstructor = true)
public final class Attribution implements Parcelable {

    /**
     * The tag of this attribution. From the &lt;manifest&gt; tag's "tag" attribute
     */
    private final @NonNull String mTag;

    /**
     * The resource ID of the label of the attribution From the &lt;manifest&gt; tag's "label"
     * attribute
     */
    private final @IdRes int mLabel;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/core/java/android/content/pm/Attribution.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    /**
     * Creates a new Attribution.
     *
     * @param tag
     *   The tag of this attribution. From the &lt;manifest&gt; tag's "tag" attribute
     * @param label
     *   The resource ID of the label of the attribution From the &lt;manifest&gt; tag's "label"
     *   attribute
     * @hide
     */
    @DataClass.Generated.Member
    public Attribution(
            @NonNull String tag,
            @IdRes int label) {
        this.mTag = tag;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mTag);
        this.mLabel = label;
        com.android.internal.util.AnnotationValidations.validate(
                IdRes.class, null, mLabel);

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * The tag of this attribution. From the &lt;manifest&gt; tag's "tag" attribute
     */
    @DataClass.Generated.Member
    public @NonNull String getTag() {
        return mTag;
    }

    /**
     * The resource ID of the label of the attribution From the &lt;manifest&gt; tag's "label"
     * attribute
     */
    @DataClass.Generated.Member
    public @IdRes int getLabel() {
        return mLabel;
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        dest.writeString(mTag);
        dest.writeInt(mLabel);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ Attribution(@NonNull Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        String tag = in.readString();
        int label = in.readInt();

        this.mTag = tag;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mTag);
        this.mLabel = label;
        com.android.internal.util.AnnotationValidations.validate(
                IdRes.class, null, mLabel);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<Attribution> CREATOR
            = new Parcelable.Creator<Attribution>() {
        @Override
        public Attribution[] newArray(int size) {
            return new Attribution[size];
        }

        @Override
        public Attribution createFromParcel(@NonNull Parcel in) {
            return new Attribution(in);
        }
    };

    @DataClass.Generated(
            time = 1683311736586L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/core/java/android/content/pm/Attribution.java",
            inputSignatures = "private final @android.annotation.NonNull java.lang.String mTag\nprivate final @android.annotation.IdRes int mLabel\nclass Attribution extends java.lang.Object implements [android.os.Parcelable]\n@com.android.internal.util.DataClass(genHiddenConstructor=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
