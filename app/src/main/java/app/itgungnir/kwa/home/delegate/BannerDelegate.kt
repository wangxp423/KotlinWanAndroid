package app.itgungnir.kwa.home.delegate

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import app.itgungnir.kwa.R
import app.itgungnir.kwa.common.WebActivity
import app.itgungnir.kwa.common.load
import app.itgungnir.kwa.common.widget.easy_adapter.BaseDelegate
import app.itgungnir.kwa.common.widget.easy_adapter.EasyAdapter
import app.itgungnir.kwa.home.HomeState
import kotlinx.android.synthetic.main.listitem_banner.view.*
import my.itgungnir.apt.router.api.Router

class BannerDelegate : BaseDelegate<HomeState.BannerVO>() {

    override fun layoutId(): Int = R.layout.listitem_banner

    @SuppressLint("SetTextI18n")
    override fun onCreateVH(container: View) {

        container.apply {
            banner.bind<HomeState.BannerVO.BannerItem>(
                layoutId = R.layout.listitem_banner_child,
                items = listOf(),
                render = { _, view, data ->
                    view.findViewById<ImageView>(R.id.image).load(data.url)
                },
                onClick = { _, data ->
                    Router.instance.with(context)
                        .target(WebActivity)
                        .addParam("title", data.title)
                        .addParam("url", data.target)
                        .go()
                },
                onPageChange = { position, totalCount, data ->
                    title.text = data.title
                    index.text = "${position + 1}/$totalCount"
                }
            )
        }
    }

    override fun onBindVH(
        item: HomeState.BannerVO,
        holder: EasyAdapter.VH,
        position: Int,
        payloads: MutableList<Bundle>
    ) {

        holder.render(item) {
            banner.update(item.items)
        }
    }
}