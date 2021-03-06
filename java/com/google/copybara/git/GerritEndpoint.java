/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara.git;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.copybara.Endpoint;
import com.google.copybara.config.SkylarkUtil;
import com.google.copybara.exception.RepoException;
import com.google.copybara.exception.ValidationException;
import com.google.copybara.git.gerritapi.ChangeInfo;
import com.google.copybara.git.gerritapi.GerritApi;
import com.google.copybara.git.gerritapi.GetChangeInput;
import com.google.copybara.git.gerritapi.IncludeResult;
import com.google.devtools.build.lib.skylarkinterface.Param;
import com.google.devtools.build.lib.skylarkinterface.SkylarkCallable;
import com.google.devtools.build.lib.skylarkinterface.SkylarkModule;
import com.google.devtools.build.lib.skylarkinterface.SkylarkModuleCategory;
import com.google.devtools.build.lib.syntax.EvalException;
import com.google.devtools.build.lib.syntax.SkylarkList;

/** Gerrit endpoint implementation for feedback migrations. */
@SuppressWarnings("unused")
@SkylarkModule(
    name = "gerrit_api_obj",
    category = SkylarkModuleCategory.BUILTIN,
    documented = false,
    doc = "Gerrit API endpoint implementation for feedback migrations.")
public class GerritEndpoint implements Endpoint {

  private final GerritOptions gerritOptions;
  private final String url;

  GerritEndpoint(GerritOptions gerritOptions, String url) {
    this.gerritOptions = Preconditions.checkNotNull(gerritOptions);
    this.url = Preconditions.checkNotNull(url);
  }

  @SkylarkCallable(
    name = "get_change",
    doc = "Retrieve a Gerrit change.",
    parameters = {
      @Param(
          name = "id", type = String.class, named = true,
          doc = "The change id or change number."
      ),
      @Param(
        name = "include_results",
        named = true,
        type = SkylarkList.class,
        generic1 = String.class,
        doc =
            ""
                + "What to include in the response. See "
                + "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html"
                + "#query-options",
        positional = false,
        defaultValue = "['LABELS']"
      ),
    }
  )
  public ChangeInfo getChange(String id, SkylarkList<?> includeResults)
      throws EvalException {
    try {
      ImmutableSet.Builder<IncludeResult> enumResults = ImmutableSet.builder();
      for (Object result : includeResults) {
        enumResults.add(
            SkylarkUtil.stringToEnum(
                null, "include_results", (String) result, IncludeResult.class));
      }
      ChangeInfo changeInfo = doGetChange(id, enumResults.build());
      ValidationException.checkCondition(
          !changeInfo.isMoreChanges(), "Pagination is not supported yet.");
      return changeInfo;
    } catch (RepoException | ValidationException | RuntimeException e) {
      throw new EvalException(/*location=*/ null, e);
    }
  }

  private ChangeInfo doGetChange(String changeId, ImmutableSet<IncludeResult> includeResults)
      throws RepoException, ValidationException {
    GerritApi gerritApi = gerritOptions.newGerritApi(url);
    return gerritApi.getChange(changeId, new GetChangeInput(includeResults));
  }

  @Override
  public ImmutableSetMultimap<String, String> describe() {
    return ImmutableSetMultimap.of("type", "gerrit_api", "url", url);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("gerritOptions", gerritOptions)
        .add("url", url)
        .toString();
  }
}
