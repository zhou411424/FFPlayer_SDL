/*****************************************************************************
 * SidebarAdapter.java
 *****************************************************************************
 * Copyright © 2012 VLC authors and VideoLAN
 * Copyright © 2012 Edward Wang
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/
package org.ffmpeg.ffplayer.ui;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.ffmpeg.ffplayer.app.FFPlayerApplication;
import org.ffmpeg.ffplayer.util.Util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import org.ffmpeg.ffplayer.R;

public class SidebarAdapter extends BaseAdapter {
    public final static String TAG = "FFPlayer/SidebarAdapter";

    static class SidebarEntryType {
        public static final String LOCAL = "local";
        public static final String NETWORK = "network";
        public static final String HISTORY = "history";
    }

    public static class SidebarEntry {
        public String id;
        String name;
        int drawableID;

        public SidebarEntry(String _id, String _name, int _drawableID) {
            this.id = _id;
            this.name = _name;
            this.drawableID = _drawableID;
        }

        public SidebarEntry(String _id, int _name, int _drawableID) {
            this.id = _id;
            this.name = FFPlayerApplication.getAppContext().getString(_name);
            this.drawableID = _drawableID;
        }
    }

    private LayoutInflater mInflater;
    public static final List<SidebarEntry> entries;
    private HashMap<String, Fragment> mFragments;
    private HashMap<String, Boolean> mFragmentAdded;
    private Semaphore mSemaphore;

    static {
        SidebarEntry entries2[] = {
            new SidebarEntry( SidebarEntryType.NETWORK, R.string.sidebar_network, R.drawable.sidebar_network ),
            new SidebarEntry( SidebarEntryType.LOCAL, R.string.sidebar_local, R.drawable.sidebar_local ),
            new SidebarEntry( SidebarEntryType.HISTORY, R.string.sidebar_history, android.R.drawable.ic_menu_recent_history ),
        };
        entries = Arrays.asList(entries2);
    }

    public SidebarAdapter() {
        mInflater = LayoutInflater.from(FFPlayerApplication.getAppContext());
        mFragments = new HashMap<String, Fragment>(entries.size());
        mFragmentAdded = new HashMap<String, Boolean>(entries.size());
        mSemaphore = new Semaphore(1, true);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        SidebarEntry sidebarEntry = entries.get(position);

        /* If view not created */
        if(v == null) {
            v = mInflater.inflate(R.layout.sidebar_item, parent, false);
        }
        TextView textView = (TextView)v;
        textView.setText(sidebarEntry.name);
        Drawable img = FFPlayerApplication.getAppResources().getDrawable(sidebarEntry.drawableID);
        if (img != null) {
            int dp_32 = Util.convertDpToPx(32);
            img.setBounds(0, 0, dp_32, dp_32);
            textView.setCompoundDrawables(img, null, null, null);
        }

        return v;
    }

    public Fragment fetchFragment(String id) {
        if(mFragments.containsKey(id) && mFragments.get(id) != null) {
            return mFragments.get(id);
        }
        Fragment f;
        if(id.equals(SidebarEntryType.NETWORK)) {
            // TODO: refine this
            f = new DirectoryViewFragment();//new AudioBrowserFragment();
        } else if(id.endsWith(SidebarEntryType.LOCAL)) {
            f = new DirectoryViewFragment();
        } else if(id.equals(SidebarEntryType.HISTORY)) {
            f = new HistoryFragment();
        } else { /* TODO */
            f = new AboutLicenceFragment();
        }
        f.setRetainInstance(true);
        mFragments.put(id, f);
        mFragmentAdded.put(id, false);
        return f;
    }

    /**
     * Has the fragment already been added?
     * Note: lock must be held prior to entering this function!
     *
     * @return true if already added
     */
    public boolean isFragmentAdded(String id) {
        return mFragmentAdded.get(id);
    }

    /**
     * Flags the fragment as added.
     *
     * @param id　ID of the fragment
     */
    public void setFragmentAdded(String id) {
        mFragmentAdded.put(id, true);
    }

    /**
     * Locks the semaphore before manipulating the added flag, since only one
     * add operation is permitted.
     *
     * Remember to unlockSemaphore() when done.
     */
    public void lockSemaphore() {
        mSemaphore.acquireUninterruptibly();
    }

    /**
     * Release the semaphore when done.
     */
    public void unlockSemaphore() {
        mSemaphore.release();
    }

    /**
     * When Android has automatically recreated a fragment from the bundle state,
     * use this function to 'restore' the recreated fragment into this sidebar
     * adapter to prevent it from trying to create the same fragment again.
     *
     * @param id ID of the fragment
     * @param f The fragment itself
     */
    public void restoreFragment(String id, Fragment f) {
        if(f == null) {
            Log.e(TAG, "Can't set null fragment for " + id + "!");
            return;
        }
        mFragments.put(id, f);
        mFragmentAdded.put(id, true);
        // if Android added it, it's been implicitly added already...
    }
}