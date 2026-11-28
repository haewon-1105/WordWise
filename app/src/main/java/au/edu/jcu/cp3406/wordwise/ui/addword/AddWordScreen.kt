package au.edu.jcu.cp3406.wordwise.ui.addword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryDefinition
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryEntry
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryMeaning
import au.edu.jcu.cp3406.wordwise.ui.theme.ButtonShape
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme
import au.edu.jcu.cp3406.wordwise.ui.theme.paperCardColors

/** Stateful entry point: search the online dictionary and add a word to the collection. */
@Composable
fun AddWordScreen(
    onBack: () -> Unit,
    viewModel: AddWordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(message.textRes))
        viewModel.onMessageShown()
    }

    AddWordContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onAdd = viewModel::addToCollection,
        onPlayPronunciation = viewModel::playPronunciation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordContent(
    uiState: AddWordUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAdd: (DictionaryMeaning, DictionaryDefinition) -> Unit,
    onPlayPronunciation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val canSearch = uiState.query.isNotBlank() && uiState.status != LookupStatus.Loading
    val submit = {
        if (canSearch) {
            focusManager.clearFocus()
            onSearch()
        }
    }

    // The outer Scaffold in WordWiseNavHost already handles system bar insets.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_word_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.add_word_intro),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.add_word_field_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(shape = ButtonShape, onClick = submit, enabled = canSearch, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_word_search))
                }
            }
            item {
                // Transparency: say exactly what leaves the device.
                Text(
                    text = stringResource(R.string.add_word_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (val status = uiState.status) {
                LookupStatus.Idle -> Unit
                LookupStatus.Loading -> item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                LookupStatus.NotFound -> item { StatusMessage(R.string.add_word_not_found) }
                LookupStatus.InvalidInput -> item { StatusMessage(R.string.add_word_invalid) }
                LookupStatus.NoConnection -> item { StatusMessage(R.string.add_word_offline) }
                LookupStatus.ServiceError -> item { StatusMessage(R.string.add_word_service_error) }
                is LookupStatus.Found -> {
                    item {
                        EntryHeader(status.entry, onPlayPronunciation)
                    }
                    status.entry.meanings.forEach { meaning ->
                        item(key = "pos-${meaning.partOfSpeech}") {
                            Text(
                                text = meaning.partOfSpeech,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        itemsIndexed(
                            items = meaning.definitions,
                            key = { index, _ -> "def-${meaning.partOfSpeech}-$index" },
                        ) { _, definition ->
                            DefinitionCard(
                                definition = definition,
                                addEnabled = !uiState.isInCollection,
                                onAdd = { onAdd(meaning, definition) },
                            )
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.add_word_attribution),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        status.entry.sourceUrl?.let { url ->
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(textRes: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = paperCardColors()) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun EntryHeader(entry: DictionaryEntry, onPlayPronunciation: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = paperCardColors()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = entry.term,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            entry.phonetic?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
            // Always available: a recording when the dictionary has one, otherwise the device voice.
            FilledTonalButton(shape = ButtonShape, onClick = onPlayPronunciation) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // The button label says what it does, so the icon is decorative.
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(stringResource(R.string.add_word_play))
                }
            }
        }
    }
}

@Composable
private fun DefinitionCard(
    definition: DictionaryDefinition,
    addEnabled: Boolean,
    onAdd: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), colors = paperCardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = definition.definition, style = MaterialTheme.typography.bodyLarge)
            definition.example?.let {
                Text(
                    text = stringResource(R.string.add_word_example, it),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            }
            OutlinedButton(shape = ButtonShape, onClick = onAdd, enabled = addEnabled) {
                Text(stringResource(R.string.add_word_add_meaning))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddWordFoundPreview() {
    val entry = DictionaryEntry(
        term = "hypothesis",
        phonetic = "/haɪˈpɒθəsɪs/",
        audioUrl = "https://example.com/hypothesis.mp3",
        meanings = listOf(
            DictionaryMeaning(
                partOfSpeech = "noun",
                definitions = listOf(
                    DictionaryDefinition(
                        definition = "A proposed explanation that can be tested.",
                        example = "The experiment supported the hypothesis.",
                    ),
                ),
            ),
        ),
        sourceUrl = "https://en.wiktionary.org/wiki/hypothesis",
    )
    WordWiseTheme {
        AddWordContent(
            uiState = AddWordUiState(query = "hypothesis", status = LookupStatus.Found(entry)),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onQueryChange = {},
            onSearch = {},
            onAdd = { _, _ -> },
            onPlayPronunciation = {},
        )
    }
}
