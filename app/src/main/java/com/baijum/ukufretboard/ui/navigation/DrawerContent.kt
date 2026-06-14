package com.baijum.ukufretboard.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.NavSection
import com.baijum.ukufretboard.ui.LocalReduceMotion

@Composable
internal fun DrawerContent(
    visibleSections: List<DrawerSection>,
    expandedState: MutableMap<String, Boolean>,
    selectedSection: NavSection,
    onItemSelected: (NavSection) -> Unit,
) {
    val navigationDrawerDescription = stringResource(R.string.cd_navigation_drawer)
    val reduceMotion = LocalReduceMotion.current
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .semantics { contentDescription = navigationDrawerDescription },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F4C6B)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_full_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        visibleSections.forEachIndexed { sectionIndex, section ->
            if (sectionIndex > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            val expanded = expandedState.getOrPut(section.title) { true }
            val expandedDescription = stringResource(R.string.cd_section_expanded)
            val collapsedDescription = stringResource(R.string.cd_section_collapsed)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expandedState[section.title] = !expanded }
                        .semantics {
                            role = Role.Button
                            stateDescription = if (expanded) expandedDescription else collapsedDescription
                        }.padding(horizontal = 28.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                Icon(
                    imageVector =
                        if (expanded) {
                            Icons.Filled.ExpandLess
                        } else {
                            Icons.Filled.ExpandMore
                        },
                    contentDescription =
                        if (expanded) {
                            stringResource(R.string.cd_collapse_section, section.title)
                        } else {
                            stringResource(R.string.cd_expand_section, section.title)
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = if (reduceMotion) EnterTransition.None else expandVertically(),
                exit = if (reduceMotion) ExitTransition.None else shrinkVertically(),
            ) {
                Column {
                    section.items.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selectedSection == item.section,
                            onClick = { onItemSelected(item.section) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.nav_help)) },
            label = { Text(stringResource(R.string.nav_help)) },
            selected = selectedSection == NavSection.HELP,
            onClick = { onItemSelected(NavSection.HELP) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.drawer_copyright),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        )
    }
}
