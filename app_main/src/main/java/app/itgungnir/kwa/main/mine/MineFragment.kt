package app.itgungnir.kwa.main.mine

import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import app.itgungnir.kwa.common.*
import app.itgungnir.kwa.common.redux.AppRedux
import app.itgungnir.kwa.common.redux.AppState
import app.itgungnir.kwa.main.R
import app.itgungnir.kwa.main.mine.add.AddArticleDialog
import kotlinx.android.synthetic.main.fragment_mine.*
import my.itgungnir.grouter.api.Router
import my.itgungnir.rxmvvm.core.mvvm.BaseFragment
import my.itgungnir.rxmvvm.core.mvvm.buildFragmentViewModel
import my.itgungnir.ui.dialog.SimpleDialog
import my.itgungnir.ui.easy_adapter.Differ
import my.itgungnir.ui.easy_adapter.EasyAdapter
import my.itgungnir.ui.easy_adapter.ListItem
import my.itgungnir.ui.easy_adapter.bind
import my.itgungnir.ui.input.ProgressButton
import my.itgungnir.ui.list_footer.ListFooter
import my.itgungnir.ui.status_view.StatusView

class MineFragment : BaseFragment() {

    private var listAdapter: EasyAdapter? = null

    private var footer: ListFooter? = null

    private val viewModel by lazy {
        buildFragmentViewModel(
            fragment = this,
            viewModelClass = MineViewModel::class.java
        )
    }

    override fun layoutId(): Int = R.layout.fragment_mine

    override fun initComponent() {

        headBar.addToolButton(getString(R.string.icon_add)) {
            AddArticleDialog().show(childFragmentManager, AddArticleDialog::class.java.name)
        }.addToolButton(getString(R.string.icon_schedule)) {
            if (AppRedux.instance.isUserIn()) {
                Router.instance.with(this)
                    .target(ScheduleActivity)
                    .go()
            } else {
                Router.instance.with(this)
                    .target(LoginActivity)
                    .go()
            }
        }.addToolButton(getString(R.string.icon_setting)) {
            Router.instance.with(this)
                .target(SettingActivity)
                .go()
        }

        minePage.apply {
            // Refresh Layout
            refreshLayout().setOnRefreshListener {
                viewModel.getMineData()
            }
            // Status View
            statusView().addDelegate(StatusView.Status.SUCCEED, R.layout.view_status_list) {
                val list = it.findViewById<RecyclerView>(R.id.list)
                // Recycler View
                listAdapter = list.bind(
                    delegate = MineArticleDelegate { id, originId ->
                        SimpleDialog(
                            bgColor = context.color(R.color.clr_dialog),
                            msgColor = context.color(R.color.text_color_level_2),
                            dividerColor = context.color(R.color.clr_divider),
                            btnColor = context.color(R.color.text_color_level_1),
                            msg = "确定要取消收藏该文章吗？",
                            onConfirm = { viewModel.disCollectArticle(id, originId) }
                        ).show(childFragmentManager, SimpleDialog::class.java.name)
                    },
                    diffAnalyzer = object : Differ {
                        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                            oldItem as MineState.MineArticleVO
                            newItem as MineState.MineArticleVO
                            return oldItem.id == newItem.id && oldItem.originId == newItem.originId
                        }

                        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                            oldItem as MineState.MineArticleVO
                            newItem as MineState.MineArticleVO
                            return oldItem.date == newItem.date
                        }

                        override fun getChangePayload(oldItem: ListItem, newItem: ListItem): Bundle? {
                            oldItem as MineState.MineArticleVO
                            newItem as MineState.MineArticleVO
                            val bundle = Bundle()
                            if (oldItem.date != newItem.date) {
                                bundle.putString("PL_DATE", newItem.date)
                            }
                            return if (bundle.isEmpty) null else bundle
                        }
                    })
                // List Footer
                footer = ListFooter.Builder()
                    .bindTo(list)
                    .render(context.color(R.color.clr_divider), context.color(R.color.clr_background))
                    .doOnLoadMore {
                        if (!minePage.refreshLayout().isRefreshing) {
                            viewModel.loadMoreMineData()
                        }
                    }
                    .build()
            }.addDelegate(StatusView.Status.EMPTY, R.layout.view_status_list_empty) {
                it.findViewById<TextView>(R.id.tip).text = "收藏列表为空，快去收藏吧~"
            }.addDelegate(StatusView.Status.FAILED, R.layout.view_status_error) {
                it.findViewById<TextView>(R.id.tip).text = "您尚未登录"
                it.findViewById<ProgressButton>(R.id.button).apply {
                    ready("登录 / 注册")
                    onAntiShakeClick {
                        Router.instance.with(context)
                            .target(LoginActivity)
                            .go()
                    }
                }
            }
        }
    }

    override fun observeVM() {

        AppRedux.instance.pick(AppState::userName, AppState::collectChanges)
            .observe(this, Observer {
                when (it?.a) {
                    null -> {
                        headBar.title("我的")
                        minePage.statusView().failed { }
                        viewModel.setState { MineState() }
                    }
                    else -> {
                        headBar.title(it.a!!)
                        viewModel.getMineData()
                    }
                }
            })

        viewModel.pick(MineState::refreshing)
            .observe(this, Observer { refreshing ->
                refreshing?.a?.let {
                    minePage.refreshLayout().isRefreshing = it
                }
            })

        viewModel.pick(MineState::items, MineState::hasMore)
            .observe(this, Observer { states ->
                states?.let {
                    if (AppRedux.instance.isUserIn()) {
                        when (it.a.isNotEmpty()) {
                            true -> minePage.statusView().succeed { listAdapter?.update(states.a) }
                            else -> minePage.statusView().empty { }
                        }
                        footer?.onLoadSucceed(it.b ?: false)
                    } else {
                        minePage.statusView().failed { }
                    }
                }
            })

        viewModel.pick(MineState::loading)
            .observe(this, Observer { loading ->
                if (loading?.a == true) {
                    footer?.onLoading()
                }
            })

        viewModel.pick(MineState::error)
            .observe(this, Observer { error ->
                error?.a?.message?.let {
                    popToast(it)
                    footer?.onLoadFailed()
                }
            })
    }
}