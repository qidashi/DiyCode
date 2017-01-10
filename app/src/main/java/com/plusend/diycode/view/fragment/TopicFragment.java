package com.plusend.diycode.view.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.plusend.diycode.R;
import com.plusend.diycode.mvp.model.base.Presenter;
import com.plusend.diycode.mvp.model.topic.entity.Topic;
import com.plusend.diycode.mvp.model.topic.presenter.TopicsPresenter;
import com.plusend.diycode.mvp.model.topic.view.TopicsView;
import com.plusend.diycode.mvp.model.user.presenter.UserTopicsPresenter;
import com.plusend.diycode.util.Constant;
import com.plusend.diycode.view.adapter.DividerListItemDecoration;
import com.plusend.diycode.view.adapter.EmptyRecyclerView;
import com.plusend.diycode.view.adapter.topic.Footer;
import com.plusend.diycode.view.adapter.topic.FooterViewProvider;
import com.plusend.diycode.view.adapter.topic.TopicViewProvider;
import java.util.List;
import me.drakeet.multitype.Items;
import me.drakeet.multitype.MultiTypeAdapter;

public class TopicFragment extends Fragment implements TopicsView {
  private static final String TAG = "TopicFragment";

  public static final String TYPE = "type";
  public static final int TYPE_ALL = 1;
  public static final int TYPE_CREATE = 2;
  public static final int TYPE_FAVORITE = 3;

  @BindView(R.id.rv) EmptyRecyclerView rv;
  @BindView(R.id.empty_view) TextView emptyView;
  private MultiTypeAdapter adapter;
  private Items items = new Items();
  private LinearLayoutManager linearLayoutManager;
  private Presenter topicsPresenter;
  private int offset = 0;
  private int type = 0;
  private String loginName;

  public static TopicFragment newInstance(String loginName, int type) {
    Log.v(TAG, "newInstance type: " + type);
    TopicFragment topicFragment = new TopicFragment();
    Bundle b = new Bundle();
    b.putString(Constant.User.LOGIN, loginName);
    b.putInt(TYPE, type);
    topicFragment.setArguments(b);
    return topicFragment;
  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView");
    View rootView = inflater.inflate(R.layout.fragment_topic, container, false);
    ButterKnife.bind(this, rootView);
    linearLayoutManager = new LinearLayoutManager(this.getContext());
    rv.setLayoutManager(linearLayoutManager);
    items.add(new Footer(Footer.STATUS_NORMAL));
    adapter = new MultiTypeAdapter(items);
    adapter.register(Topic.class, new TopicViewProvider());
    adapter.register(Footer.class, new FooterViewProvider());
    rv.setAdapter(adapter);
    rv.addItemDecoration(new DividerListItemDecoration(getContext()));
    rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
      private int lastVisibleItem;

      @Override public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (newState == RecyclerView.SCROLL_STATE_IDLE
            && lastVisibleItem + 1 == adapter.getItemCount()) {
          ((Footer) items.get(items.size() - 1)).setStatus(Footer.STATUS_LOADING);
          adapter.notifyItemChanged(adapter.getItemCount());
          if (type == TYPE_ALL) {
            ((TopicsPresenter) topicsPresenter).getTopics(offset);
          } else if (type == TYPE_CREATE) {
            ((UserTopicsPresenter) topicsPresenter).getUserCreateTopics(loginName, offset);
          } else if (type == TYPE_FAVORITE) {
            ((UserTopicsPresenter) topicsPresenter).getUserFavoriteTopics(loginName, offset);
          }
        }
      }

      @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
      }
    });
    rv.setEmptyView(emptyView);

    return rootView;
  }

  @Override public void showTopics(List<Topic> topicList) {
    if (topicList == null) {
      Log.v(TAG, "showTopics: null");
      return;
    }
    Log.v(TAG, "showTopics: " + topicList.size());
    for (Topic topic : topicList) {
      // 插入 FooterView 前面
      items.add(items.size() - 1, topic);
      adapter.notifyItemInserted(adapter.getItemCount() - 1);
    }
    offset = items.size() - 1;
    if (topicList.size() < 20) {
      ((Footer) items.get(items.size() - 1)).setStatus(Footer.STATUS_NO_MORE);
      adapter.notifyItemChanged(adapter.getItemCount());
    } else {
      ((Footer) items.get(items.size() - 1)).setStatus(Footer.STATUS_NORMAL);
      adapter.notifyItemChanged(adapter.getItemCount());
    }
  }

  @Override public void onStart() {
    Log.d(TAG, "onStart");
    super.onStart();

    Bundle bundle = getArguments();
    if (bundle != null) {
      loginName = bundle.getString(Constant.User.LOGIN);
      type = bundle.getInt(TYPE);
      Log.d(TAG, "loginName: " + loginName + " type: " + type);
    }

    Log.d(TAG, "type: " + type);
    if (type == TYPE_ALL) {
      topicsPresenter = new TopicsPresenter(this);
    } else if (type == TYPE_CREATE) {
      topicsPresenter = new UserTopicsPresenter(this);
    } else if (type == TYPE_FAVORITE) {
      topicsPresenter = new UserTopicsPresenter(this);
    }
    topicsPresenter.start();
    if (!TextUtils.isEmpty(loginName)) {
      if (type != TYPE_FAVORITE) {
        ((UserTopicsPresenter) topicsPresenter).getUserCreateTopics(loginName, offset);
      } else {
        ((UserTopicsPresenter) topicsPresenter).getUserFavoriteTopics(loginName, offset);
      }
    } else {
      ((TopicsPresenter) topicsPresenter).getTopics(offset);
    }
  }

  @Override public void onStop() {
    if (topicsPresenter != null) {
      topicsPresenter.stop();
    }
    super.onStop();
  }
}
