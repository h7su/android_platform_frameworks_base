/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.os;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.platform.test.annotations.Presubmit;
import android.util.Log;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

/**
 * Unit tests for bundle that requires accessing hidden APS.  Tests that can be written only with
 * public APIs should go in the CTS counterpart.
 *
 * Run with: atest FrameworksCoreTests:android.os.BundleTest
 */
@SmallTest
@Presubmit
@RunWith(AndroidJUnit4.class)
public class BundleTest {
    private Log.TerribleFailureHandler mWtfHandler;

    @After
    public void tearDown() throws Exception {
        BaseBundle.setShouldDefuse(false);
        if (mWtfHandler != null) {
            Log.setWtfHandler(mWtfHandler);
        }
    }

    /**
     * Take a bundle, write it to a parcel and return the parcel.
     */
    private Parcel getParcelledBundle(Bundle bundle) {
        final Parcel p = Parcel.obtain();
        // Don't use p.writeParcelabe(), which would write the creator, which we don't need.
        bundle.writeToParcel(p, 0);
        p.setDataPosition(0);
        return p;
    }

    /**
     * Create a test bundle, parcel it and return the parcel.
     */
    private Parcel createBundleParcel(boolean withFd) throws Exception {
        final Bundle source = new Bundle();
        source.putString("string", "abc");
        source.putInt("int", 1);
        if (withFd) {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            pipe[1].close();
            source.putParcelable("fd", pipe[0]);
        }
        return getParcelledBundle(source);
    }

    /**
     * Verify a bundle generated by {@link #createBundleParcel(boolean)}.
     */
    private void checkBundle(Bundle b, boolean withFd) {
        // First, do the checks without actually unparceling the bundle.
        // (Note looking into the contents will unparcel a bundle, so we'll do it later.)
        assertTrue("mParcelledData shouldn't be null here.", b.isParcelled());

        // Make sure FLAG_HAS_FDS and FLAG_HAS_FDS_KNOWN are set/cleared properly.
        if (withFd) {
            // FLAG_HAS_FDS and FLAG_HAS_FDS_KNOWN should both be set.
            assertEquals(Bundle.FLAG_HAS_FDS | Bundle.FLAG_HAS_FDS_KNOWN,
                    b.mFlags & (Bundle.FLAG_HAS_FDS | Bundle.FLAG_HAS_FDS_KNOWN));
        } else {
            // FLAG_HAS_FDS_KNOWN should be set, bot not FLAG_HAS_FDS.
            assertEquals(Bundle.FLAG_HAS_FDS_KNOWN,
                    b.mFlags & (Bundle.FLAG_HAS_FDS | Bundle.FLAG_HAS_FDS_KNOWN));
        }

        // Then, check the contents.
        assertEquals("abc", b.getString("string"));
        assertEquals(1, b.getInt("int"));

        // Make sure FLAG_HAS_FDS and FLAG_HAS_FDS_KNOWN are set/cleared properly.
        if (withFd) {
            assertEquals(ParcelFileDescriptor.class, b.getParcelable("fd").getClass());
            assertEquals(3, b.keySet().size());
        } else {
            assertEquals(2, b.keySet().size());
        }
        assertFalse(b.isParcelled());
    }

    @Test
    public void testCreateFromParcel() throws Exception {
        boolean withFd;
        Parcel p;
        Bundle b;
        int length;

        withFd = false;

        // new Bundle with p
        p = createBundleParcel(withFd);
        checkBundle(new Bundle(p), withFd);
        p.recycle();

        // new Bundle with p and length
        p = createBundleParcel(withFd);
        length = p.readInt();
        checkBundle(new Bundle(p, length), withFd);
        p.recycle();

        // readFromParcel()
        p = createBundleParcel(withFd);
        b = new Bundle();
        b.readFromParcel(p);
        checkBundle(b, withFd);
        p.recycle();

        // Same test with FDs.
        withFd = true;

        // new Bundle with p
        p = createBundleParcel(withFd);
        checkBundle(new Bundle(p), withFd);
        p.recycle();

        // new Bundle with p and length
        p = createBundleParcel(withFd);
        length = p.readInt();
        checkBundle(new Bundle(p, length), withFd);
        p.recycle();

        // readFromParcel()
        p = createBundleParcel(withFd);
        b = new Bundle();
        b.readFromParcel(p);
        checkBundle(b, withFd);
        p.recycle();
    }

    @Test
    public void kindofEquals_bothUnparcelled_same() {
        Bundle bundle1 = new Bundle();
        bundle1.putString("StringKey", "S");
        bundle1.putInt("IntKey", 2);

        Bundle bundle2 = new Bundle();
        bundle2.putString("StringKey", "S");
        bundle2.putInt("IntKey", 2);

        assertTrue(BaseBundle.kindofEquals(bundle1, bundle2));
    }

    @Test
    public void kindofEquals_bothUnparcelled_different() {
        Bundle bundle1 = new Bundle();
        bundle1.putString("StringKey", "S");
        bundle1.putInt("IntKey", 2);

        Bundle bundle2 = new Bundle();
        bundle2.putString("StringKey", "T");
        bundle2.putLong("LongKey", 30L);

        assertFalse(BaseBundle.kindofEquals(bundle1, bundle2));
    }

    @Test
    public void kindofEquals_bothParcelled_same() {
        Bundle bundle1 = new Bundle();
        bundle1.putString("StringKey", "S");
        bundle1.putInt("IntKey", 2);
        bundle1.readFromParcel(getParcelledBundle(bundle1));

        Bundle bundle2 = new Bundle();
        bundle2.putString("StringKey", "S");
        bundle2.putInt("IntKey", 2);
        bundle2.readFromParcel(getParcelledBundle(bundle2));

        assertTrue(bundle1.isParcelled());
        assertTrue(bundle2.isParcelled());
        assertTrue(BaseBundle.kindofEquals(bundle1, bundle2));
    }

    @Test
    public void kindofEquals_bothParcelled_different() {
        Bundle bundle1 = new Bundle();
        bundle1.putString("StringKey", "S");
        bundle1.putInt("IntKey", 2);
        bundle1.readFromParcel(getParcelledBundle(bundle1));

        Bundle bundle2 = new Bundle();
        bundle2.putString("StringKey", "T");
        bundle2.putLong("LongKey", 5);
        bundle2.readFromParcel(getParcelledBundle(bundle2));

        assertTrue(bundle1.isParcelled());
        assertTrue(bundle2.isParcelled());
        assertFalse(BaseBundle.kindofEquals(bundle1, bundle2));
    }

    @Test
    public void kindofEquals_ParcelledUnparcelled_empty() {
        Bundle bundle1 = new Bundle();
        bundle1.readFromParcel(getParcelledBundle(bundle1));

        Bundle bundle2 = new Bundle();

        assertTrue(bundle1.isParcelled());
        assertFalse(bundle2.isParcelled());
        // Even though one is parcelled and the other is not, both are empty, so it should
        // return true
        assertTrue(BaseBundle.kindofEquals(bundle1, bundle2));
    }

    @Test
    public void kindofEquals_lazyValues() {
        Parcelable p1 = new CustomParcelable(13, "Tiramisu");
        Parcelable p2 = new CustomParcelable(13, "Tiramisu");

        // 2 maps with live objects
        Bundle a = new Bundle();
        a.putParcelable("key1", p1);
        Bundle b = new Bundle();
        b.putParcelable("key1", p2);
        assertTrue(Bundle.kindofEquals(a, b));

        // 2 identical parcels
        a.readFromParcel(getParcelledBundle(a));
        a.setClassLoader(getClass().getClassLoader());
        b.readFromParcel(getParcelledBundle(b));
        b.setClassLoader(getClass().getClassLoader());
        assertTrue(Bundle.kindofEquals(a, b));

        // 2 lazy values with identical parcels inside
        a.isEmpty();
        b.isEmpty();
        assertTrue(Bundle.kindofEquals(a, b));

        // 1 lazy value vs 1 live object
        a.getParcelable("key1");
        assertFalse(Bundle.kindofEquals(a, b));

        // 2 live objects
        b.getParcelable("key1");
        assertTrue(Bundle.kindofEquals(a, b));
    }

    @Test
    public void kindofEquals_lazyValuesWithIdenticalParcels_returnsTrue() {
        Parcelable p1 = new CustomParcelable(13, "Tiramisu");
        Parcelable p2 = new CustomParcelable(13, "Tiramisu");
        Bundle a = new Bundle();
        a.putParcelable("key1", p1);
        a.readFromParcel(getParcelledBundle(a));
        a.setClassLoader(getClass().getClassLoader());
        Bundle b = new Bundle();
        // Adding extra element so that the position of the elements of interest in their respective
        // source parcels are different so we can cover that case of Parcel.compareData(). We'll
        // remove the element later so the map is equal.
        b.putString("key0", "string");
        b.putParcelable("key1", p2);
        b.readFromParcel(getParcelledBundle(b));
        b.setClassLoader(getClass().getClassLoader());
        a.isEmpty();
        b.isEmpty();
        b.remove("key0");
        // 2 lazy values with identical parcels inside

        assertTrue(Bundle.kindofEquals(a, b));
    }

    @Test
    public void kindofEquals_lazyValuesAndDifferentClassLoaders_returnsFalse() {
        Parcelable p1 = new CustomParcelable(13, "Tiramisu");
        Parcelable p2 = new CustomParcelable(13, "Tiramisu");
        Bundle a = new Bundle();
        a.putParcelable("key", p1);
        a.readFromParcel(getParcelledBundle(a));
        a.setClassLoader(getClass().getClassLoader());
        Bundle b = new Bundle();
        b.putParcelable("key", p2);
        b.readFromParcel(getParcelledBundle(b));
        b.setClassLoader(Bundle.class.getClassLoader()); // BCP
        // 2 lazy values with identical parcels inside
        a.isEmpty();
        b.isEmpty();

        assertFalse(Bundle.kindofEquals(a, b));
    }

    @Test
    public void kindofEquals_lazyValuesOfDifferentTypes_returnsFalse() {
        Parcelable p = new CustomParcelable(13, "Tiramisu");
        Parcelable[] ps = {p};
        Bundle a = new Bundle();
        a.putParcelable("key", p);
        a.readFromParcel(getParcelledBundle(a));
        a.setClassLoader(getClass().getClassLoader());
        Bundle b = new Bundle();
        b.putParcelableArray("key", ps);
        b.readFromParcel(getParcelledBundle(b));
        b.setClassLoader(getClass().getClassLoader());
        a.isEmpty();
        b.isEmpty();

        assertFalse(Bundle.kindofEquals(a, b));
    }

    @Test
    public void kindofEquals_lazyValuesWithDifferentLengths_returnsFalse() {
        Parcelable p1 = new CustomParcelable(13, "Tiramisu");
        Parcelable p2 = new CustomParcelable(13, "Tiramisuuuuuuuu");
        Bundle a = new Bundle();
        a.putParcelable("key", p1);
        a.readFromParcel(getParcelledBundle(a));
        a.setClassLoader(getClass().getClassLoader());
        Bundle b = new Bundle();
        b.putParcelable("key", p2);
        b.readFromParcel(getParcelledBundle(b));
        b.setClassLoader(getClass().getClassLoader());
        a.isEmpty();
        b.isEmpty();

        assertFalse(Bundle.kindofEquals(a, b));
    }

    @Test
    public void readWriteLengthMismatch_logsWtf() throws Exception {
        mWtfHandler = Log.setWtfHandler((tag, e, system) -> {
            throw new RuntimeException(e);
        });
        Parcelable parcelable = new CustomParcelable(13, "Tiramisu").setHasLengthMismatch(true);
        Bundle bundle = new Bundle();
        bundle.putParcelable("p", parcelable);
        bundle.readFromParcel(getParcelledBundle(bundle));
        bundle.setClassLoader(getClass().getClassLoader());
        RuntimeException e = assertThrows(RuntimeException.class, () -> bundle.getParcelable("p"));
        assertThat(e.getCause()).isInstanceOf(Log.TerribleFailure.class);
    }

    @Test
    public void getParcelable_whenThrowingAndNotDefusing_throws() throws Exception {
        Bundle.setShouldDefuse(false);
        Bundle bundle = new Bundle();
        bundle.putParcelable("key", new CustomParcelable(13, "Tiramisu"));
        bundle.readFromParcel(getParcelledBundle(bundle));

        // Default class-loader is the bootpath class-loader, which doesn't contain
        // CustomParcelable, so trying to read it will throw BadParcelableException.
        assertThrows(BadParcelableException.class, () -> bundle.getParcelable("key"));
    }

    @Test
    public void getParcelable_whenThrowingAndDefusing_returnsNull() throws Exception {
        Bundle.setShouldDefuse(true);
        Bundle bundle = new Bundle();
        bundle.putParcelable("key", new CustomParcelable(13, "Tiramisu"));
        bundle.putString("string", "value");
        bundle.readFromParcel(getParcelledBundle(bundle));

        // Default class-loader is the bootpath class-loader, which doesn't contain
        // CustomParcelable, so trying to read it will throw BadParcelableException.
        assertThat(bundle.<Parcelable>getParcelable("key")).isNull();
        // Doesn't affect other items
        assertThat(bundle.getString("string")).isEqualTo("value");
    }

    @Test
    public void getParcelable_whenThrowingAndDefusing_leavesElement() throws Exception {
        Bundle.setShouldDefuse(true);
        Bundle bundle = new Bundle();
        Parcelable parcelable = new CustomParcelable(13, "Tiramisu");
        bundle.putParcelable("key", parcelable);
        bundle.putString("string", "value");
        bundle.readFromParcel(getParcelledBundle(bundle));
        assertThat(bundle.<Parcelable>getParcelable("key")).isNull();

        // Now, we simulate reserializing and assign the proper class loader to not throw anymore
        bundle.readFromParcel(getParcelledBundle(bundle));
        bundle.setClassLoader(getClass().getClassLoader());

        // We're able to retrieve it even though we failed before
        assertThat(bundle.<Parcelable>getParcelable("key")).isEqualTo(parcelable);
    }

    @Test
    public void partialDeserialization_whenNotDefusing_throws() throws Exception {
        Bundle.setShouldDefuse(false);
        Bundle bundle = getMalformedBundle();
        assertThrows(BadParcelableException.class, bundle::isEmpty);
    }

    @Test
    public void partialDeserialization_whenDefusing_emptiesMap() throws Exception {
        Bundle.setShouldDefuse(true);
        Bundle bundle = getMalformedBundle();
        bundle.isEmpty();
        // Nothing thrown
        assertThat(bundle.size()).isEqualTo(0);
    }

    private Bundle getMalformedBundle() {
        Parcel p = Parcel.obtain();
        p.writeInt(BaseBundle.BUNDLE_MAGIC);
        int start = p.dataPosition();
        p.writeInt(1); // Number of items
        p.writeString("key");
        p.writeInt(131313); // Invalid type
        p.writeInt(0); // Anything, really
        int end = p.dataPosition();
        p.setDataPosition(0);
        return new Bundle(p, end - start);
    }


    private static class CustomParcelable implements Parcelable {
        public final int integer;
        public final String string;
        public boolean hasLengthMismatch;

        CustomParcelable(int integer, String string) {
            this.integer = integer;
            this.string = string;
        }

        protected CustomParcelable(Parcel in) {
            integer = in.readInt();
            string = in.readString();
            hasLengthMismatch = in.readBoolean();
        }

        public CustomParcelable setHasLengthMismatch(boolean hasLengthMismatch) {
            this.hasLengthMismatch = hasLengthMismatch;
            return this;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(integer);
            out.writeString(string);
            out.writeBoolean(hasLengthMismatch);
            if (hasLengthMismatch) {
                out.writeString("extra-write");
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CustomParcelable)) {
                return false;
            }
            CustomParcelable
                    that = (CustomParcelable) other;
            return integer == that.integer
                    && hasLengthMismatch == that.hasLengthMismatch
                    && string.equals(that.string);
        }

        @Override
        public int hashCode() {
            return Objects.hash(integer, string, hasLengthMismatch);
        }

        public static final Creator<CustomParcelable> CREATOR = new Creator<CustomParcelable>() {
            @Override
            public CustomParcelable createFromParcel(Parcel in) {
                return new CustomParcelable(in);
            }
            @Override
            public CustomParcelable[] newArray(int size) {
                return new CustomParcelable[size];
            }
        };
    }
}
