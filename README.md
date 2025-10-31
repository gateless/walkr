[![Build Status](https://github.com/gateless/walkr/actions/workflows/clojure.yml/badge.svg)](https://github.com/gateless/walkr/actions/workflows/clojure.yml)

# _About_

Walkr (walk-reduce) is a Clojure library built to help you easily walk-reduce clojure data structures.

# _Usage_

Here's a simple example.

```clj
(ns user
  (:require [walkr.core :as w]
            [clojure.string :as str]))

(w/prewalk-reduce
  (fn [acc item]
    (if (string? item)
      [(conj acc (str/lower-case item)) (str/upper-case item)]
      [acc item]))
  #{}
  [{:foo {:bar {:value #{"cOlD"}}
                      :other "hOt"}}
   {:foo {:bar {:value "ColDer"}
          :other "hoTteR"}}])
;;; =>
;;; [#{"cold" "hot" "colder" "hotter"}
;;;  [{:foo {:bar {:value #{"COLD"}}
;;;          :other "HOT"}}
;;;   {:foo {:bar {:value "COLDER"}
;;;          :other "HOTTER"}}]]
```

See the existing tests for more examples.

# _Building_

walkr is built, tested, and deployed using [Clojure Tools Deps](https://clojure.org/guides/deps_and_cli).

GNU Make is used to simplify invocation of some commands.

# _Availability_

gateless/walkr releases for this project are on [Clojars](https://clojars.org/). Simply add the following to your project:

[![Clojars Project](https://clojars.org/com.github.gateless/walkr/latest-version.svg)](http://clojars.org/com.github.gateless/walkr)

# _Communication_

- For any other questions or issues about walkr free to browse or open a [Github Issue](https://github.com/gateless/walkr/issues).

# Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

# YourKit

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/dotnet-profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

# LICENSE

Copyright 2025 Gateless

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

&nbsp;&nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
