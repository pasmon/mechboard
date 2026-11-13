package io.github.pasmon.mechboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Settings screen listing the words in the [PersonalDictionary], with a field
 * to add a word and a delete button on each. Opened from [SettingsFragment].
 *
 * Changes take effect in the keyboard immediately: both use the same
 * [PersonalDictionary] instance.
 */
class PersonalDictionaryFragment : Fragment(R.layout.fragment_personal_dictionary) {

    private lateinit var dictionary: PersonalDictionary
    private lateinit var newWord: EditText
    private lateinit var emptyMessage: TextView
    private lateinit var wordList: ListView
    private lateinit var adapter: WordAdapter

    private val listener = PersonalDictionary.Listener { showWords() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dictionary = PersonalDictionary.of(requireContext())
        newWord = view.findViewById(R.id.new_word)
        emptyMessage = view.findViewById(R.id.empty_message)
        wordList = view.findViewById(R.id.word_list)
        adapter = WordAdapter()
        wordList.adapter = adapter

        view.findViewById<Button>(R.id.add_word).setOnClickListener { addTypedWord() }
        newWord.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTypedWord()
                true
            } else {
                false
            }
        }

        dictionary.addListener(listener)
        showWords()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.pref_personal_dictionary_title)
    }

    override fun onDestroyView() {
        dictionary.removeListener(listener)
        super.onDestroyView()
    }

    /** Saves the word in the field, or explains on the field why it cannot be saved. */
    private fun addTypedWord() {
        val word = PersonalDictionary.normalize(newWord.text.toString())
        when {
            word == null -> newWord.error = getString(R.string.personal_dictionary_invalid)
            dictionary.contains(word) ->
                newWord.error = getString(R.string.personal_dictionary_duplicate, word)
            else -> {
                dictionary.add(word)
                newWord.text.clear()
            }
        }
    }

    private fun showWords() {
        adapter.clear()
        adapter.addAll(dictionary.words)
        val empty = dictionary.words.isEmpty()
        emptyMessage.visibility = if (empty) View.VISIBLE else View.GONE
        wordList.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private inner class WordAdapter : ArrayAdapter<String>(requireContext(), 0) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_personal_word, parent, false)
            val word = getItem(position) ?: return row
            row.findViewById<TextView>(R.id.word).text = word
            row.findViewById<ImageButton>(R.id.delete_word).apply {
                contentDescription = getString(R.string.personal_dictionary_delete, word)
                setOnClickListener { dictionary.remove(word) }
            }
            return row
        }
    }
}
