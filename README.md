# Balloons Manager

## Requirements

* Java 21

## Setup

Use the same configs as in [ICPC Live Overlay v3](https://github.com/icpc/live-v3). That's all!

Examples:
- [CLICS format (ICPC WF)](https://github.com/icpc/live-v3/blob/main/config/_examples/clics/settings.json)
- [Codeforces](https://github.com/icpc/live-v3/blob/main/config/_examples/codeforces/settings.json)

Also you can customize data received from test system. We support
[all options](https://github.com/icpc/live-v3/blob/main/docs/advanced.json.md) from ICPC Live, but you most likely
need to:
* [set balloon colors for each problem](https://github.com/icpc/live-v3/blob/main/docs/advanced.json.md#change-problem-info)
* put `custom-fields.csv` in the config directory with columns `team_id,hall,place`
  * place will be displayed near the team name so you'll be able to find the team at the contest floor
  * if you run distributed contest (or just have many rooms in the same building),
    you’ll be able to filter teams by their hall

## Launch

When using ZIP release, you can use provided scripts:

```bash
./balloons.sh  # Unix-like systems
balloons.bat   # Windows
```

Also you can just launch it manually:

```bash
java -jar balloons.jar -c path/to/config
```

You can customize a few options:

* By default, port is 8001, but you can set another one via `--port=1234`.

* By default, registration is enabled (but you still need to approve everyone), you can block self-registration using `--disable-registration`.

In Shell/Batch scripts those variables are changed inside the script.

Then navigate in your browser to `http://{ip}:{port}/` (by default and from the same machine, http://localhost:8001/). If this service
is exposed to the internet, it's strictly recommended to use some reverse proxy like nginx. See [config example](release-archive/examples/nginx_site.conf).

Don't forget to add admin user (see below).

> We create H2 database that contains a few files. They are created in config directory and start with `h2`, e.g. `h2.trace.db`.
> [Read more](http://www.h2database.com/html/features.html#database_file_layout) about database files.
>  
> You may want to `.gitignore` them if you're committing configs to some repository.

## CLI

Replace `./balloons.sh` with `balloons.bat` or `java -jar balloons.jar -c path/to/config` depending on your setup.

```bash
# Create a volunteer
./balloons.sh volunteer create login password

# Create an admin
./balloons.sh volunteer create --admin login password

# Make the volunteer an admin
./balloons.sh volunteer update login --manage=true

# Change password
./balloons.sh volunteer update login --new-password=password

# Delete volunteer (only if they did not take any balloons)
./balloons.sh volunteer delete login

# Database SQL shell
./balloons.sh h2shell
```

> [!IMPORTANT]
> When specifying flags, make sure that `-c` / `--config-directory` is set **before** the command (`h2shell` / `volunteer`)
> and other options (username, password) are set **after** the command.

## Development

### Frontend

You can launch frontend in development mode:

```bash
cd frontend/
pnpm run dev
```

It will start at port 5173. To avoid CORS-tricks, it proxies `/api/` to `http://localhost:8001`. If backend is located somewhere else,
set `BACKEND_URL` environment variable. See [vite.config.ts](frontend/vite.config.ts) for details.

### Build

This task should do the trick.

```bash
gradle shadowJar
```

ZIP archive is generated using

```bash
gradle release
```

### Translation Guide

1. Add `frontend/src/i18n/<your-lang-code>.ts` file. Use the same structure as other localization files.
2. Add your language in `src/main/kotlin/org/icpclive/balloons/BalloonOptions.kt`.

Voilà! I'm waiting for your pull requests.

## License

You may use this repository code according to [The MIT License](LICENSE) terms.

## Credits

This project is inspired by previous versions — [neerc/balloons](https://github.com/neerc/balloons) and [nikkirche/balloons-reborn](https://github.com/nikkirche/balloons-reborn).

All of this wouldn't work without [ICPC Live tools](https://github.com/icpc/live-v3).

## TODO

- [ ] Store time of run and delivery and show it in interface
- [ ] Tests
