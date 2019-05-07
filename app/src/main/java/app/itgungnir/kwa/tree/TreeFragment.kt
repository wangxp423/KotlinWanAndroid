package app.itgungnir.kwa.tree

import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import app.itgungnir.kwa.R
import app.itgungnir.kwa.common.dp2px
import app.itgungnir.kwa.common.popToast
import app.itgungnir.kwa.common.widget.easy_adapter.EasyAdapter
import app.itgungnir.kwa.common.widget.easy_adapter.bind
import app.itgungnir.kwa.common.widget.status_view.StatusView
import app.itgungnir.kwa.tree.tools.ToolsDialog
import kotlinx.android.synthetic.main.fragment_tree.*
import my.itgungnir.rxmvvm.core.mvvm.BaseFragment
import my.itgungnir.rxmvvm.core.mvvm.buildActivityViewModel
import org.jetbrains.anko.bottomPadding

class TreeFragment : BaseFragment() {

    private val viewModel by lazy {
        buildActivityViewModel(
            activity = activity!!,
            viewModelClass = TreeViewModel::class.java
        )
    }

    private var listAdapter: EasyAdapter? = null

    override fun layoutId(): Int = R.layout.fragment_tree

    override fun initComponent() {
        // Head Bar
        headBar.title("知识体系")
            .addToolButton("\ue834") {
                ToolsDialog().show(childFragmentManager, ToolsDialog::class.java.name)
            }
            .addToolButton("\ue744") {
                // TODO 导航
            }
        // Common Page
        treePage.apply {
            // Swipe Refresh Layout
            refreshLayout().setOnRefreshListener {
                viewModel.getTreeList()
            }
            // Status View
            statusView().addDelegate(StatusView.Status.SUCCEED, R.layout.status_view_list) {
                it.findViewById<RecyclerView>(R.id.list).apply {
                    bottomPadding = context.dp2px(10.0F)
                    listAdapter = this.bind(delegate = TreeDelegate())
                }
            }.addDelegate(StatusView.Status.EMPTY, R.layout.status_view_list_empty) {
                it.findViewById<TextView>(R.id.tip).text = "还没有知识体系~"
            }
        }
        // Init Data
        viewModel.getTreeList()
    }

    override fun observeVM() {

        viewModel.pick(TreeState::refreshing)
            .observe(this, Observer { refreshing ->
                refreshing?.a?.let {
                    treePage.refreshLayout().isRefreshing = it
                }
            })

        viewModel.pick(TreeState::items)
            .observe(this, Observer { items ->
                items?.a?.let { data ->
                    treePage.statusView().succeed { listAdapter?.update(data) }
                }
            })

        viewModel.pick(TreeState::error)
            .observe(this, Observer { error ->
                error?.a?.message?.let {
                    popToast(it)
                    treePage.statusView().empty { }
                }
            })
    }
}