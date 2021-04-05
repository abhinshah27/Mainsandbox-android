package it.keybiz.lbsapp.corporate.features.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.models.UserGenericLocation
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper
import kotlinx.android.synthetic.main.item_profile_location.view.*

class UserWithLocationAdapter(diffUtilCallback: UserGenericLocationDiffCallback, val listener: NearMeItemInteractionListener?):
        ListAdapter<UserGenericLocation, UserWithLocationAdapter.UserLocationVH>(diffUtilCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserLocationVH {
        return UserLocationVH(LayoutInflater.from(parent.context).inflate(R.layout.item_profile_location, parent, false))
    }

    override fun onBindViewHolder(holder: UserLocationVH, position: Int) {
        holder.setUser(getItem(position))
    }

    override fun submitList(list: List<UserGenericLocation>?) {
        super.submitList(if (list != null) ArrayList(list) else null)
    }

    inner class UserLocationVH(itemView: View): RecyclerView.ViewHolder(itemView) {

        private var currentUser: UserGenericLocation? = null

        init {
            itemView.setOnClickListener {
                listener?.goToProfile(ProfileHelper.ProfileType.NOT_FRIEND, currentUser?.userID)
            }
        }

        fun setUser(user: UserGenericLocation?) {
            currentUser = user

            if (!user?.avatarURL.isNullOrBlank())
                MediaHelper.loadProfilePictureWithPlaceholder(itemView.context, user?.avatarURL, itemView.profile_picture as? ImageView)
            else
                (itemView.profile_picture as? ImageView)?.setImageResource(R.drawable.ic_profile_placeholder)

            itemView.name?.text = user?.name
            itemView.distance?.text = user?.distance
        }
    }

    override fun getItemId(position: Int): Long {
        return if (getItem(position) != null) getItem(position).hashCode().toLong() else super.getItemId(position)
    }

    interface NearMeItemInteractionListener {
        fun goToProfile(type: ProfileHelper.ProfileType?, id: String?)
    }

}


class UserGenericLocationDiffCallback: DiffUtil.ItemCallback<UserGenericLocation>() {

    override fun areItemsTheSame(oldItem: UserGenericLocation, newItem: UserGenericLocation): Boolean {
        return oldItem.userID == newItem.userID
    }

    override fun areContentsTheSame(oldItem: UserGenericLocation, newItem: UserGenericLocation): Boolean {
        return (
                oldItem.avatarURL == newItem.avatarURL &&
                        oldItem.name == newItem.name &&
                        oldItem.distance == newItem.distance
                )
    }
}