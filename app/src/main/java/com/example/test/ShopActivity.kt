package com.example.test

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.data.AppRepository
import com.example.test.data.PurchaseOutcome
import com.example.test.data.ShopCatalog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.concurrent.Executors

/** A small local point shop. Repository transactions are the authority for purchases. */
class ShopActivity : AppCompatActivity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var balanceView: TextView
    private lateinit var selectedView: TextView
    private lateinit var messageView: TextView
    private lateinit var container: LinearLayout
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)
        applySystemBarInsets()
        setupBottomNavigation(R.id.nav_myroom)
        balanceView = findViewById(R.id.shop_balance)
        selectedView = findViewById(R.id.shop_selected_title)
        messageView = findViewById(R.id.shop_message)
        container = findViewById(R.id.shop_items)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() = perform { null }

    private fun perform(operation: AppRepository.() -> String?) {
        if (busy || isDestroyed || isFinishing) return
        busy = true
        setButtonsEnabled(false)
        io.execute {
            val result = runCatching {
                AppRepository(applicationContext).use { repository ->
                    val message = operation(repository)
                    ShopSnapshot(repository.balance(), repository.ownedItemIds(), repository.appliedItemId(), message)
                }
            }
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                busy = false
                result.fold(
                    onSuccess = { render(it) },
                    onFailure = {
                        messageView.setText(R.string.shop_storage_error)
                        messageView.visibility = View.VISIBLE
                        setButtonsEnabled(true)
                    }
                )
            }
        }
    }

    private fun render(snapshot: ShopSnapshot) {
        balanceView.text = getString(R.string.shop_balance, snapshot.balance)
        selectedView.text = ShopCatalog.find(snapshot.appliedId ?: "")?.let {
            getString(R.string.shop_selected, it.title)
        } ?: getString(R.string.shop_none_selected)
        messageView.text = snapshot.message.orEmpty()
        messageView.visibility = if (snapshot.message == null) View.GONE else View.VISIBLE
        container.removeAllViews()
        ShopCatalog.items.forEach { item ->
            val card = MaterialCardView(this).apply {
                radius = dp(18).toFloat()
                cardElevation = 0f
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
            }
            content.addView(TextView(this).apply {
                text = item.title
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            })
            content.addView(TextView(this).apply {
                text = item.description
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                setPadding(0, dp(8), 0, dp(8))
            })
            val owned = item.id in snapshot.ownedIds
            val applied = snapshot.appliedId == item.id
            content.addView(TextView(this).apply {
                text = if (owned) getString(R.string.shop_owned) else getString(R.string.shop_price, item.price)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            })
            content.addView(MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
                minHeight = dp(48)
                text = getString(if (applied) R.string.shop_applied else if (owned) R.string.shop_apply else R.string.shop_buy)
                isEnabled = !applied
                contentDescription = getString(R.string.shop_item_action, item.title, text)
                tag = applied
                setOnClickListener {
                    perform {
                        if (owned) {
                            getString(if (applyItem(item.id)) R.string.shop_apply_success else R.string.shop_apply_failed, item.title)
                        } else {
                            when (purchase(item.id)) {
                                PurchaseOutcome.PURCHASED -> getString(R.string.shop_purchase_success, item.title)
                                PurchaseOutcome.ALREADY_OWNED -> getString(R.string.shop_already_owned)
                                PurchaseOutcome.INSUFFICIENT_FUNDS -> getString(R.string.shop_insufficient)
                                PurchaseOutcome.UNKNOWN_ITEM -> getString(R.string.shop_unknown_item)
                            }
                        }
                    }
                }
            })
            card.addView(content)
            container.addView(card)
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        fun visit(view: View) {
            if (view is MaterialButton) view.isEnabled = enabled && view.tag != true
            if (view is android.view.ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        if (::container.isInitialized) visit(container)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }

    private data class ShopSnapshot(val balance: Long, val ownedIds: Set<String>, val appliedId: String?, val message: String?)
}
