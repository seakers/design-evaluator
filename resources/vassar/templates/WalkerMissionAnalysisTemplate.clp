(deffacts all-rev-times


{% for item in items %}
    (DATABASE::Revisit-time-of
        (mission-architecture {{item.mission_architecture()}})
        (num-of-planes# {{item.num_planes()}})
        (num-of-sats-per-plane# {{item.sats_per_plane()}})
        (orbit-altitude# {{item.orbit_altitude()}})
        (orbit-inclination {{item.orbit_inclination()}})
        (instrument-field-of-view# {{item.instrument_fov()}})


        (avg-revisit-time-global# {% if item.avg_revisit_time_global().equalsIgnoreCase("NaN") %}nil{% else %}{{item.avg_revisit_time_global()}}{% endif %})
        (avg-revisit-time-tropics# {% if item.avg_revisit_time_tropics().equalsIgnoreCase("NaN") %}nil{% else %}{{item.avg_revisit_time_tropics()}}{% endif %})
        (avg-revisit-time-northern-hemisphere# {% if item.avg_revisit_time_northern_hemisphere().equalsIgnoreCase("NaN") %}nil{% else %}{{item.avg_revisit_time_northern_hemisphere()}}{% endif %})
        (avg-revisit-time-southern-hemisphere# {% if item.avg_revisit_time_southern_hemisphere().equalsIgnoreCase("NaN") %}nil{% else %}{{item.avg_revisit_time_southern_hemisphere()}}{% endif %})
        (avg-revisit-time-cold-regions# {% if item.avg_revisit_time_cold_regiouis().equalsIgnoreCase("NaN") %}nil{% else %}{{item.avg_revisit_time_cold_regiouis()}}{% endif %})
        (avg-revisit-time-US# {% if item.avg_revisit_time_us().equalsIgnoreCase("NaN") %}nil{% else %}{{item.avg_revisit_time_us()}}{% endif %})
        (factHistory 000)

        ; (orbit-type {{item.mission_architecture()}})
        ; (orbit-raan {{item.mission_architecture()}})
    )
{% endfor %}
)