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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import app.intra.R;
import app.intra.net.dns.DnsPacket;
import app.intra.net.doh.Transaction;
import app.intra.sys.firebase.LogWrapper;
import com.google.common.net.InternetDomainName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

/**
 * Adapter for displaying DNS query history in a RecyclerView with filter support
 */
public class QueryHistoryAdapter extends RecyclerView.Adapter<QueryHistoryAdapter.TransactionViewHolder> {

  private Context context;
  private CountryMap countryMap = null;
  private List<Transaction> allTransactions = new ArrayList<>();
  private List<Transaction> filteredTransactions = new ArrayList<>();
  
  private final int condensedColor, expandedColor;

  QueryHistoryAdapter(Context context) {
    super();
    this.context = context;
    condensedColor = context.getResources().getColor(R.color.light);
    expandedColor = context.getResources().getColor(R.color.floating);
  }

  private void activateCountryMap() {
    if (countryMap != null) {
      return;
    }
    if (context instanceof QueryHistoryActivity) {
      try {
        countryMap = new CountryMap(((QueryHistoryActivity) context).getAssets());
      } catch (IOException e) {
        LogWrapper.logException(e);
      }
    }
  }

  public void reset(@Nullable Queue<Transaction> transactions) {
    allTransactions.clear();
    if (transactions != null) {
      allTransactions.addAll(transactions);
    }
    filteredTransactions.clear();
    filteredTransactions.addAll(allTransactions);
    notifyDataSetChanged();
  }

  public void add(Transaction transaction) {
    allTransactions.add(0, transaction);
    // Check if this transaction matches current filter
    filteredTransactions.add(0, transaction);
    notifyItemInserted(0);
  }

  public void filter(String searchQuery, Transaction.Status statusFilter) {
    filteredTransactions.clear();
    
    for (Transaction transaction : allTransactions) {
      boolean matchesSearch = searchQuery == null || searchQuery.isEmpty() ||
          (transaction.name != null && transaction.name.toLowerCase().contains(searchQuery.toLowerCase()));
      
      boolean matchesStatus = statusFilter == null ||
          (statusFilter == Transaction.Status.COMPLETE && transaction.status == Transaction.Status.COMPLETE) ||
          (statusFilter == Transaction.Status.SEND_FAIL && transaction.status != Transaction.Status.COMPLETE);
      
      if (matchesSearch && matchesStatus) {
        filteredTransactions.add(transaction);
      }
    }
    
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return filteredTransactions.size();
  }

  @NonNull
  @Override
  public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.transaction_row, parent, false);
    return new TransactionViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
    Transaction transaction = filteredTransactions.get(position);
    holder.update(transaction);
  }

  public class TransactionViewHolder extends RecyclerView.ViewHolder {
    private final TextView hostname;
    private final TextView flag;
    private final TextView latency;
    private final ToggleButton expand;
    private final View details;

    private Transaction transaction = null;

    TransactionViewHolder(View v) {
      super(v);
      hostname = v.findViewById(R.id.hostname);
      flag = v.findViewById(R.id.flag);
      latency = v.findViewById(R.id.latency);
      expand = v.findViewById(R.id.expand);
      details = v.findViewById(R.id.details);

      expand.setOnClickListener(view -> {
        if (details.getVisibility() == View.VISIBLE) {
          details.setVisibility(View.GONE);
          itemView.setBackgroundColor(condensedColor);
        } else {
          details.setVisibility(View.VISIBLE);
          itemView.setBackgroundColor(expandedColor);
        }
      });
    }

    void update(Transaction transaction) {
      this.transaction = transaction;
      hostname.setText(transaction.name);
      
      // Set latency
      if (transaction.status == Transaction.Status.COMPLETE && transaction.responseTime > 0) {
        long latencyMs = transaction.responseTime - transaction.queryTime;
        latency.setText(String.format(Locale.getDefault(), "%d ms", latencyMs));
        latency.setTextColor(context.getResources().getColor(R.color.accent_good));
      } else {
        latency.setText(R.string.query_failed);
        latency.setTextColor(context.getResources().getColor(R.color.accent_bad));
      }

      // Set flag
      activateCountryMap();
      if (countryMap != null && transaction.response != null) {
        try {
          DnsPacket parsed = new DnsPacket(transaction.response);
          String flag = getFlag(parsed);
          if (flag != null) {
            this.flag.setText(flag);
          } else {
            this.flag.setText("");
          }
        } catch (Exception e) {
          this.flag.setText("");
        }
      } else {
        this.flag.setText("");
      }

      // Update detailed view
      updateDetails();
      
      // Reset expansion state
      details.setVisibility(View.GONE);
      expand.setChecked(false);
      itemView.setBackgroundColor(condensedColor);
    }

    private void updateDetails() {
      TextView queryType = itemView.findViewById(R.id.type);
      TextView queryTime = itemView.findViewById(R.id.time);
      TextView responseStatus = itemView.findViewById(R.id.response);
      
      // Set query type
      String typeStr = "A";
      if (transaction.type == 28) {
        typeStr = "AAAA";
      }
      queryType.setText(typeStr);
      
      // Set query time
      if (transaction.responseCalendar != null) {
        queryTime.setText(String.format(Locale.getDefault(), "%tT", transaction.responseCalendar));
      } else {
        queryTime.setText("");
      }
      
      // Set response status
      if (transaction.status != null) {
        responseStatus.setText(transaction.status.toString());
        if (transaction.status == Transaction.Status.COMPLETE) {
          responseStatus.setTextColor(context.getResources().getColor(R.color.accent_good));
        } else {
          responseStatus.setTextColor(context.getResources().getColor(R.color.accent_bad));
        }
      }
    }

    private @Nullable String getFlag(DnsPacket parsed) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        return null;
      }
      InetAddress firstAddress = getFirstAddress(parsed);
      if (firstAddress == null) {
        return null;
      }
      String country = countryMap.getCountry(firstAddress);
      if (country == null) {
        return null;
      }
      return CountryMap.getFlag(country);
    }

    private @Nullable InetAddress getFirstAddress(DnsPacket parsed) {
      if (parsed.getResponseCode() != 0) {
        return null;
      }
      List<InetAddress> ips = parsed.getResponseAddresses();
      if (ips == null || ips.isEmpty()) {
        return null;
      }
      return ips.get(0);
    }
  }
}
