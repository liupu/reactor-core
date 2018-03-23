/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.util.debug;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities around manipulating stack traces and displaying assembly traces.
 *
 * @author Simon Baslé
 */
public class Traces {

	/**
	 * Strip an operator name of various prefixes and suffixes.
	 * @param name the operator name, usually simpleClassName or fully-qualified classname.
	 * @return the stripped operator name
	 */
	public static final String stripOperatorName(String name) {
		if (name.contains("@") && name.contains("$")) {
			name = name.substring(0, name.indexOf('$'));
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		String stripped = name
				.replaceAll("Parallel|Flux|Mono|Publisher|Subscriber", "")
				.replaceAll("Fuseable|Operator|Conditional", "");

		if(stripped.length() > 0) {
			return stripped.substring(0, 1).toLowerCase() + stripped.substring(1);
		}
		return stripped;
	}

	public static String extractOperatorAssemblyInformation(String source) {
		return extractOperatorAssemblyInformation(source, false);
	}

	public static String extractOperatorAssemblyInformation(String source, boolean skipFirst) {
		String[] uncleanTraces = source.split("\n");
		final List<String> traces = Stream.of(uncleanTraces)
		                                  .map(String::trim)
		                                  .filter(s -> !s.isEmpty())
		                                  .skip(skipFirst ? 1 : 0)
		                                  .collect(Collectors.toList());

		if (traces.isEmpty()) {
			return "[no operator assembly information]";
		}

		int i = 0;
		while (i < traces.size() && traces.get(i).startsWith("reactor.core.publisher") && !traces.get(i).contains("Test")) {
			i++;
		}

		String apiLine;
		String userCodeLine;
		if (i == 0) {
			//no line was a reactor API line
			apiLine = "";
			userCodeLine = traces.get(0);
		}
		else if (i == traces.size()) {
			//we skipped ALL lines, meaning they're all reactor API lines. We'll fully display the last one
			apiLine = "";
			userCodeLine = traces.get(i-1);
		}
		else {
			//currently on user code line, previous one is API
			apiLine = traces.get(i - 1);
			userCodeLine = traces.get(i);
		}

		//now we want something in the form "Flux.map in user.code.Class.method(Class.java:123)"
		if (apiLine.isEmpty()) return userCodeLine;

		int linePartIndex = apiLine.indexOf('(');
		if (linePartIndex > 0) {
			apiLine = apiLine.substring(0, linePartIndex);
		}
		apiLine = apiLine.replaceFirst("reactor.core.publisher.", "");

		return apiLine + " ⇢ " + userCodeLine;
	}

}
