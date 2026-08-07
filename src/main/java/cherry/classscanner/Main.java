/*
 * Copyright 2025,2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.classscanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {@code doMain}はコンテキストをtry-with-resourcesでクローズしたうえで
 * {@link SpringApplication#exit}を呼び出す。これはApplicationContextが保持する
 * {@link org.springframework.boot.ExitCodeGenerator}Bean(本アプリでは{@link ClassScannerRunner})の
 * 終了コードを、コンテキストクローズ後も正しくOSへ伝播させるためのSpring Bootの標準的なCLIパターン。
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        System.exit(doMain(args));
    }

    private static int doMain(String[] args) {
        try (var context = SpringApplication.run(Main.class, args)) {
            return SpringApplication.exit(context);
        }
    }
}
