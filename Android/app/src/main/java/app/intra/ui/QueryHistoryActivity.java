/*
Copyright 2024 Jigsaw Operations LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package app.intra.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.intra.R;
import app.intra.net.doh.Transaction;
import app.intra.sys.InternalNames;
import app.intra.sys.VpnController;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity for displaying DNS query history with search and filter capabilities
 */
public class QueryHistoryActivity extends AppCompatActivity {

  private RecyclerView recyclerView;
  private QueryHistoryAdapter adapter;
  private RecyclerView.LayoutManager layoutManager;
  private SearchView searchView;
  private Spinner filterSpinner;
  private String currentSearchQuery = "";
  private Transaction.Status currentFilter = null;

  private BroadcastReceiver messageReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (InternalNames.RESULT.name().equals(intent.getAction())) {
            Transaction transaction = 
                (Transaction) intent.getSerializableExtra(InternalNames.TRANSACTION.name());
            if (transaction != null) {
              adapter.add(transaction);
              applyFilters();
            }
          }
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_query_history);

    // Set up the toolbar
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle(R.string.query_history_title);
    }

    // Set up the recycler view
    recyclerView = findViewById(R.id.history_recycler);
    recyclerView.setHasFixedSize(true);
    layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new QueryHistoryAdapter(this);
    adapter.reset(VpnController.getInstance().getTracker(this).getRecentTransactions());
    recyclerView.setAdapter(adapter);

    // Set up search view
    searchView = findViewById(R.id.search_view);
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        currentSearchQuery = query;
        applyFilters();
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        currentSearchQuery = newText;
        applyFilters();
        return true;
      }
    });

    // Set up filter spinner
    filterSpinner = findViewById(R.id.status_filter_spinner);
    String[] filterOptions = {
        getString(R.string.filter_all),
        getString(R.string.filter_complete),
        getString(R.string.filter_failed)
    };
    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
        this, android.R.layout.simple_spinner_item, filterOptions);
    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    filterSpinner.setAdapter(spinnerAdapter);
    filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
          case 0: // All
            currentFilter = null;
            break;
          case 1: // Complete
            currentFilter = Transaction.Status.COMPLETE;
            break;
          case 2: // Failed
            // Show all failed statuses (not COMPLETE)
            currentFilter = Transaction.Status.SEND_FAIL;
            break;
        }
        applyFilters();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        currentFilter = null;
        applyFilters();
      }
    });

    // Register broadcast receiver
    IntentFilter intentFilter = new IntentFilter(InternalNames.RESULT.name());
    LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
  }

  @Override
  protected void onDestroy() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    super.onDestroy();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void applyFilters() {
    adapter.filter(currentSearchQuery, currentFilter);
  }
}
